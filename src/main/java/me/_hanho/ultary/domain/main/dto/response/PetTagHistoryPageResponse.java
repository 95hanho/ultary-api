package me._hanho.ultary.domain.main.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PetTagHistoryPageResponse {

	private List<PetTagHistoryItemResponse> items;
}
