package me._hanho.ultary.domain.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me._hanho.ultary.domain.ai.AiService;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;
import me._hanho.ultary.domain.tag.model.Tag;
import me._hanho.ultary.security.principal.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class TagServiceRecommendTest {

	@Mock
	private TagMapper tagMapper;

	@Mock
	private FileService fileService;

	@Mock
	private AiService aiService;

	@InjectMocks
	private TagService tagService;

	@Test
	void recommend_usesAiTagsAndLinksExisting() {
		UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
		when(principal.getUserNo()).thenReturn(101L);
		when(aiService.recommendHashtags(101L, "한강 산책")).thenReturn(List.of("한강산책", "신규태그"));

		Tag existing = new Tag();
		existing.setTagId(10L);
		existing.setHashtag("한강산책");
		existing.setPurpose("USER");
		existing.setProfileStatus("NONE");
		existing.setUseCount(3);
		when(tagMapper.findActiveByHashtag("한강산책")).thenReturn(existing);
		when(tagMapper.findActiveByHashtag("신규태그")).thenReturn(null);

		List<TagResponse> result = tagService.recommend(principal, "한강 산책", 5);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getTagId()).isEqualTo(10L);
		assertThat(result.get(0).getHashtag()).isEqualTo("한강산책");
		assertThat(result.get(1).getTagId()).isNull();
		assertThat(result.get(1).getHashtag()).isEqualTo("신규태그");
		verify(tagMapper, never()).recommend(anyString(), anyInt());
	}

	@Test
	void recommend_aiEmpty_fallsBackToDb() {
		UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
		when(principal.getUserNo()).thenReturn(101L);
		when(aiService.recommendHashtags(101L, "roy")).thenReturn(List.of());

		Tag db = new Tag();
		db.setTagId(1L);
		db.setHashtag("royalcanin");
		db.setPurpose("COMMERCIAL");
		db.setProfileStatus("NONE");
		when(tagMapper.recommend(eq("roy"), eq(5))).thenReturn(List.of(db));

		List<TagResponse> result = tagService.recommend(principal, "roy", 10);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getHashtag()).isEqualTo("royalcanin");
	}

	@Test
	void recommend_blankQuery_usesDbOnly() {
		UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
		when(tagMapper.recommend(isNull(), eq(5))).thenReturn(List.of());

		assertThat(tagService.recommend(principal, "  ", 5)).isEmpty();
		verify(aiService, never()).recommendHashtags(org.mockito.ArgumentMatchers.any(), anyString());
	}
}
