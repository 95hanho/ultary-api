package me._hanho.ultary.domain.dm.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DmShareTargetListResponse {

	private List<DmShareTargetItemResponse> items;
}
