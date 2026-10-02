package me._hanho.ultary.domain.activity.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivityListResponse {

	private List<ActivityItemResponse> items;
}
