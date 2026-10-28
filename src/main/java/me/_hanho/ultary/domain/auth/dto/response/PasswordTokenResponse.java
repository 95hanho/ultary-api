package me._hanho.ultary.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordTokenResponse {
    private boolean selectionRequired;
    private java.util.List<TokenResponse.AccountChoice> accounts;

	private String passwordChangeToken;
}
