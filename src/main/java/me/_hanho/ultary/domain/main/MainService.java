package me._hanho.ultary.domain.main;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.NotImplemented;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.story.dto.response.StoryOwnerResponse;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

@Service
@RequiredArgsConstructor
public class MainService {

	private final StoryService storyService;

	@Transactional(readOnly = true)
	public List<StoryOwnerResponse> getStoryOwners(UserPrincipal principal) {
		return storyService.listResidentOwners(principal);
	}

	@Transactional(readOnly = true)
	public List<StoryResponse> getStories(UserPrincipal principal, Long userNo) {
		return storyService.listByUser(principal, userNo);
	}

	public void getFeeds() {
		NotImplemented.yet();
	}

	public void search(String q, String type) {
		NotImplemented.yet();
	}
}
