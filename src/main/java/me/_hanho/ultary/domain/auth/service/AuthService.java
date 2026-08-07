package me._hanho.ultary.domain.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.auth.dto.request.ChangePasswordRequest;
import me._hanho.ultary.domain.auth.dto.request.LoginRequest;
import me._hanho.ultary.domain.auth.dto.request.PasswordTokenRequest;
import me._hanho.ultary.domain.auth.dto.request.PhoneAuthRequest;
import me._hanho.ultary.domain.auth.dto.request.PhoneVerifyRequest;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.request.SignupRequest;
import me._hanho.ultary.domain.auth.dto.request.SocialLoginRequest;
import me._hanho.ultary.domain.auth.dto.request.UpdateMeRequest;
import me._hanho.ultary.domain.auth.dto.response.MeResponse;
import me._hanho.ultary.domain.auth.dto.response.PasswordTokenResponse;
import me._hanho.ultary.domain.auth.dto.response.PhoneAuthResponse;
import me._hanho.ultary.domain.auth.dto.response.PhoneVerifyResponse;
import me._hanho.ultary.domain.auth.dto.response.SocialLoginResponse;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;
import me._hanho.ultary.domain.auth.mapper.TokenMapper;
import me._hanho.ultary.domain.auth.model.SocialProvider;
import me._hanho.ultary.domain.auth.model.Token;
import me._hanho.ultary.domain.auth.support.PhoneAuthCodeStore;
import me._hanho.ultary.domain.user.mapper.UserMapper;
import me._hanho.ultary.domain.user.mapper.UserSocialMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.domain.user.model.UserSocial;
import me._hanho.ultary.security.jwt.JwtProperties;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.jwt.TokenOwnerType;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String NICKNAME_RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
	private static final int NICKNAME_RANDOM_LENGTH = 8;
	private static final int NICKNAME_MAX_RETRY = 10;

	private final UserMapper userMapper;
	private final UserSocialMapper userSocialMapper;
	private final TokenMapper tokenMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final PhoneAuthCodeStore phoneAuthCodeStore;
	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * FE/BFF가 소셜 OAuth로 확보한 providerUserId로 DB 조회/가입 후 JWT 발급.
	 * 제공자 API 키/토큰 검증은 FE 책임.
	 */
	@Transactional
	public SocialLoginResponse socialLogin(SocialLoginRequest request, HttpServletRequest httpRequest) {
		SocialProvider provider = SocialProvider.from(request.getProvider());
		String providerUserId = request.getProviderUserId().trim();
		String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null;
		String name = StringUtils.hasText(request.getName()) ? request.getName().trim() : null;

		UserSocial linked = userSocialMapper.findByProviderAndProviderUserId(provider.name(), providerUserId);
		boolean newUser = false;
		User user;

		if (linked != null) {
			user = userMapper.findActiveByUserNo(linked.getUserNo());
			if (user == null) {
				throw new BusinessException(ErrorCode.USER_INACTIVE);
			}
		} else {
			user = createSocialUser(provider, providerUserId, email, name);
			newUser = true;
		}

		TokenResponse tokens = issueTokens(user, httpRequest);
		boolean defaultNickname = Boolean.TRUE.equals(user.getIsDefaultNickname());

		return SocialLoginResponse.builder()
				.accessToken(tokens.getAccessToken())
				.refreshToken(tokens.getRefreshToken())
				.tokenType(tokens.getTokenType())
				.expiresIn(tokens.getExpiresIn())
				.newUser(newUser)
				.defaultNickname(defaultNickname)
				.build();
	}

	@Transactional
	public void linkSocial(UserPrincipal principal, SocialLoginRequest request) {
		SocialProvider provider = SocialProvider.from(request.getProvider());
		String providerUserId = request.getProviderUserId().trim();
		String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null;

		User user = userMapper.findActiveByUserNo(principal.getUserNo());
		if (user == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		if (userSocialMapper.findByUserNoAndProvider(user.getUserNo(), provider.name()) != null) {
			throw new BusinessException(ErrorCode.SOCIAL_ALREADY_LINKED);
		}

		UserSocial existing = userSocialMapper.findByProviderAndProviderUserId(provider.name(), providerUserId);
		if (existing != null) {
			throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_IN_USE);
		}

		UserSocial social = new UserSocial();
		social.setUserNo(user.getUserNo());
		social.setProvider(provider.name());
		social.setProviderUserId(providerUserId);
		social.setProviderEmail(email);
		userSocialMapper.insert(social);
	}

	@Transactional
	public void unlinkSocial(UserPrincipal principal, String providerValue) {
		SocialProvider provider = SocialProvider.from(providerValue);

		User user = userMapper.findActiveByUserNo(principal.getUserNo());
		if (user == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		UserSocial linked = userSocialMapper.findByUserNoAndProvider(user.getUserNo(), provider.name());
		if (linked == null) {
			throw new BusinessException(ErrorCode.SOCIAL_NOT_LINKED);
		}

		int socialCount = userSocialMapper.countByUserNo(user.getUserNo());
		boolean hasPassword = StringUtils.hasText(user.getPassword());
		if (!hasPassword && socialCount <= 1) {
			throw new BusinessException(ErrorCode.CANNOT_UNLINK_LAST_LOGIN);
		}

		userSocialMapper.deleteByUserNoAndProvider(user.getUserNo(), provider.name());
	}

	@Transactional
	public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
		User user = resolveLoginUser(request);
		if (user == null
				|| !StringUtils.hasText(user.getPassword())
				|| !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.LOGIN_FAILED);
		}
		if (!"ACTIVE".equals(user.getWithdrawalStatus())) {
			throw new BusinessException(ErrorCode.USER_INACTIVE);
		}

		return issueTokens(user, httpRequest);
	}

	@Transactional
	public TokenResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
		Claims claims = jwtTokenProvider.parseRefreshToken(request.getRefreshToken());
		if (jwtTokenProvider.getOwnerType(claims) != TokenOwnerType.USER) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		Long userNo = jwtTokenProvider.getSubjectAsLong(claims);
		Token storedToken = tokenMapper.findValidUserRefreshToken(userNo, request.getRefreshToken());
		if (storedToken == null) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		User user = userMapper.findActiveByUserNo(userNo);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_INACTIVE);
		}

		tokenMapper.revokeByTokenId(storedToken.getTokenId());
		return issueTokens(user, httpRequest);
	}

	@Transactional
	public void logout(UserPrincipal principal) {
		tokenMapper.revokeAllByUserNo(principal.getUserNo());
	}

	@Transactional(readOnly = true)
	public MeResponse me(UserPrincipal principal) {
		User user = userMapper.findActiveByUserNo(principal.getUserNo());
		if (user == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return toMeResponse(user);
	}

	@Transactional
	public MeResponse updateMe(UserPrincipal principal, UpdateMeRequest request) {
		User user = userMapper.findActiveByUserNo(principal.getUserNo());
		if (user == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		if (StringUtils.hasText(request.getNickname())
				&& !request.getNickname().equals(user.getNickname())
				&& userMapper.countByNickname(request.getNickname(), user.getUserNo()) > 0) {
			throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
		}

		if (StringUtils.hasText(request.getNickname())) {
			user.setNickname(request.getNickname());
			user.setIsDefaultNickname(false);
		}
		if (request.getName() != null) {
			user.setName(request.getName());
		}
		if (request.getEmail() != null) {
			user.setEmail(request.getEmail());
		}
		if (request.getBio() != null) {
			user.setBio(request.getBio());
		}
		if (request.getRegionSido() != null) {
			user.setRegionSido(request.getRegionSido());
		}
		if (request.getRegionSigungu() != null) {
			user.setRegionSigungu(request.getRegionSigungu());
		}

		userMapper.updateProfile(user);
		return toMeResponse(userMapper.findActiveByUserNo(user.getUserNo()));
	}

	@Transactional
	public void withdraw(UserPrincipal principal) {
		User user = userMapper.findActiveByUserNo(principal.getUserNo());
		if (user == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		userMapper.updateWithdrawalStatus(user.getUserNo(), "WITHDRAWN");
		tokenMapper.revokeAllByUserNo(user.getUserNo());
	}

	@Transactional
	public void signup(SignupRequest request) {
		Claims claims = jwtTokenProvider.parsePhoneAuthCompleteToken(request.getPhoneAuthCompleteToken());
		String phone = jwtTokenProvider.getPhone(claims);

		if (userMapper.findByPhone(phone) != null) {
			throw new BusinessException(ErrorCode.PHONE_ALREADY_USED);
		}
		if (userMapper.countByNickname(request.getNickname(), null) > 0) {
			throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
		}

		User user = new User();
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setNickname(request.getNickname());
		user.setIsDefaultNickname(false);
		user.setName(StringUtils.hasText(request.getName()) ? request.getName() : null);
		user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail() : null);
		user.setPhone(phone);
		userMapper.insert(user);
	}

	public PhoneAuthResponse requestPhoneAuth(PhoneAuthRequest request) {
		String phone = request.getPhone();
		String code = String.format("%06d", secureRandom.nextInt(1_000_000));
		long ttlSeconds = jwtProperties.getExpiration().getPhoneauth();

		phoneAuthCodeStore.save(phone, code, ttlSeconds);
		// TODO: SMS 연동. 로컬 확인용으로 인증번호 로그 출력
		log.info("[phoneAuth] phone={}, code={}", phone, code);

		String phoneAuthToken = jwtTokenProvider.createPhoneAuthToken(phone);
		return PhoneAuthResponse.builder().phoneAuthToken(phoneAuthToken).build();
	}

	public PhoneVerifyResponse verifyPhoneAuth(PhoneVerifyRequest request) {
		Claims claims = jwtTokenProvider.parsePhoneAuthToken(request.getPhoneAuthToken());
		String phone = jwtTokenProvider.getPhone(claims);

		if (!phoneAuthCodeStore.matches(phone, request.getCode())) {
			throw new BusinessException(ErrorCode.PHONE_AUTH_FAILED);
		}
		phoneAuthCodeStore.remove(phone);

		String completeToken = jwtTokenProvider.createPhoneAuthCompleteToken(phone);
		return PhoneVerifyResponse.builder().phoneAuthCompleteToken(completeToken).build();
	}

	@Transactional(readOnly = true)
	public PasswordTokenResponse createPasswordToken(PasswordTokenRequest request) {
		Claims claims = jwtTokenProvider.parsePhoneAuthCompleteToken(request.getPhoneAuthCompleteToken());
		String phone = jwtTokenProvider.getPhone(claims);
		if (!phone.equals(request.getPhone())) {
			throw new BusinessException(ErrorCode.PHONE_AUTH_FAILED);
		}

		User user = userMapper.findByPhone(phone);
		if (user == null || !"ACTIVE".equals(user.getWithdrawalStatus())) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		String token = jwtTokenProvider.createPasswordChangeToken(user.getUserNo());
		return PasswordTokenResponse.builder().passwordChangeToken(token).build();
	}

	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		Claims claims = jwtTokenProvider.parsePasswordChangeToken(request.getPasswordChangeToken());
		Long userNo = jwtTokenProvider.getSubjectAsLong(claims);

		User user = userMapper.findActiveByUserNo(userNo);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		userMapper.updatePassword(userNo, passwordEncoder.encode(request.getNewPassword()));
		tokenMapper.revokeAllByUserNo(userNo);
	}

	private User createSocialUser(
			SocialProvider provider,
			String providerUserId,
			String email,
			String name) {
		// email UNIQUE — 이미 다른 계정이 사용 중이면 신규 소셜 유저에는 넣지 않음
		String resolvedEmail = email;
		if (resolvedEmail != null && userMapper.findByEmail(resolvedEmail) != null) {
			resolvedEmail = null;
		}

		User user = new User();
		user.setPassword(null);
		user.setName(name);
		user.setNickname(generateUniqueNickname(provider));
		user.setIsDefaultNickname(true);
		user.setEmail(resolvedEmail);
		user.setPhone(null);
		userMapper.insert(user);

		UserSocial social = new UserSocial();
		social.setUserNo(user.getUserNo());
		social.setProvider(provider.name());
		social.setProviderUserId(providerUserId);
		social.setProviderEmail(email);
		userSocialMapper.insert(social);

		return user;
	}

	private String generateUniqueNickname(SocialProvider provider) {
		for (int i = 0; i < NICKNAME_MAX_RETRY; i++) {
			String nickname = provider.nicknamePrefix() + randomCode(NICKNAME_RANDOM_LENGTH);
			if (userMapper.countByNickname(nickname, null) == 0) {
				return nickname;
			}
		}
		throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	private String randomCode(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(NICKNAME_RANDOM_CHARS.charAt(secureRandom.nextInt(NICKNAME_RANDOM_CHARS.length())));
		}
		return sb.toString();
	}

	private User resolveLoginUser(LoginRequest request) {
		if (StringUtils.hasText(request.getEmail())) {
			return userMapper.findByEmail(request.getEmail().trim());
		}
		if (StringUtils.hasText(request.getPhone())) {
			return userMapper.findByPhone(request.getPhone().trim());
		}
		return null;
	}

	private MeResponse toMeResponse(User user) {
		return MeResponse.builder()
				.userNo(user.getUserNo())
				.name(user.getName())
				.nickname(user.getNickname())
				.defaultNickname(Boolean.TRUE.equals(user.getIsDefaultNickname()))
				.hasPassword(StringUtils.hasText(user.getPassword()))
				.email(user.getEmail())
				.phone(user.getPhone())
				.profileFileId(user.getProfileFileId())
				.bio(user.getBio())
				.regionSido(user.getRegionSido())
				.regionSigungu(user.getRegionSigungu())
				.withdrawalStatus(user.getWithdrawalStatus())
				.createdAt(user.getCreatedAt())
				.build();
	}

	private TokenResponse issueTokens(User user, HttpServletRequest httpRequest) {
		String accessToken = jwtTokenProvider.createUserAccessToken(user.getUserNo());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserNo(), TokenOwnerType.USER);

		Token token = new Token();
		token.setOwnerType(TokenOwnerType.USER.name());
		token.setUserNo(user.getUserNo());
		token.setConnectIp(resolveClientIp(httpRequest));
		token.setConnectAgent(trimAgent(httpRequest.getHeader("User-Agent")));
		token.setRefreshToken(refreshToken);
		token.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiresInSeconds()));
		tokenMapper.insert(token);

		return TokenResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.tokenType("Bearer")
				.expiresIn(jwtTokenProvider.getAccessTokenExpiresInSeconds())
				.build();
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String trimAgent(String userAgent) {
		if (userAgent == null) {
			return null;
		}
		return userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent;
	}
}
