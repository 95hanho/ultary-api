package me._hanho.ultary.domain.main.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 검색 후 들어간 울타리 주인 */
@Getter
@Setter
public class SaveSearchHistoryRequest {

	@NotNull(message = "targetUserNo는 필수입니다.")
	private Long targetUserNo;
}
