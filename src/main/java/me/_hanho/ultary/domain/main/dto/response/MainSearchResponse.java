package me._hanho.ultary.domain.main.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;

@Getter
@Builder
public class MainSearchResponse {

	private List<MainSearchUserItem> users;
	private List<MainSearchPetItem> pets;
	private List<TagResponse> tags;
	private List<FeedResponse> feeds;
}
