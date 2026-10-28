package me._hanho.ultary.domain.myultary;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.*;
import me._hanho.ultary.domain.user.*;
import me._hanho.ultary.domain.settings.PrivacyService;

@Service @RequiredArgsConstructor
public class UltaryLinkService {
    private final UltaryLinkMapper mapper;
    private final UserMapper users;
    private final UserBlockMapper blocks;
    private final PrivacyService privacy;
    public record Link(Long userNo, String nickname, boolean available) {}
    @Transactional(readOnly=true)
    public List<Link> list(Long owner, Long viewer) {
        if (users.findActiveByUserNo(owner) == null || (viewer == null && privacy.isPrivateAccount(owner))
            || (viewer != null && blocks.findActiveByPair(owner,viewer) != null)) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        boolean own = owner.equals(viewer);
        return mapper.list(owner).stream().map(row -> new Link(row.userNo(), row.nickname(),
            "ACTIVE".equals(row.withdrawalStatus()) && !privacy.isPrivateAccount(row.userNo())
            && (viewer == null || blocks.findActiveByPair(row.userNo(), viewer) == null)))
            .filter(link -> own || link.available()).toList();
    }
    @Transactional(readOnly=true)
    public List<Link> search(Long viewer, String query) {
        String term = query == null ? "" : query.trim().replaceFirst("^@+", "");
        if (term.isEmpty()) return List.of();
        return users.searchActiveByNickname(viewer, term, 50).stream()
            .filter(user -> !user.getUserNo().equals(viewer) && !privacy.isPrivateAccount(user.getUserNo())
                && blocks.findActiveEitherWay(viewer,user.getUserNo()) == null)
            .map(user -> new Link(user.getUserNo(),user.getNickname(),true)).toList();
    }
    @Transactional
    public void add(Long owner, Long target) {
        if (users.lockActiveUser(owner) == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        if (owner.equals(target)) throw new BusinessException(ErrorCode.INVALID_INPUT,"자신의 울타리는 연결할 수 없습니다.");
        if (users.findActiveByUserNo(target) == null || privacy.isPrivateAccount(target)
            || blocks.findActiveEitherWay(owner,target) != null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        mapper.insert(owner,target,mapper.nextOrder(owner));
    }
    @Transactional public void delete(Long owner, Long target) { mapper.delete(owner,target); }
}
