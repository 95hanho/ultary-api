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
	@Mock private me._hanho.ultary.domain.pet.PetMapper petMapper;
	@Mock private me._hanho.ultary.domain.feed.FeedService feedService;

	@InjectMocks
	private TagService tagService;

	@Test
	void recommend_usesAiTagsAndLinksExisting() {
		UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
		when(principal.getUserNo()).thenReturn(101L);
		when(tagMapper.recommend(null, 100)).thenReturn(List.of());
		when(aiService.recommendHashtags(101L, "한강 산책", List.of())).thenReturn(List.of("신규태그", "한강산책", "한강산책", "신규태그"));

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
		when(tagMapper.recommend(null, 100)).thenReturn(List.of());
		when(aiService.recommendHashtags(101L, "roy", List.of())).thenReturn(List.of());

		Tag db = new Tag();
		db.setTagId(1L);
		db.setHashtag("royalcanin");
		db.setPurpose("COMMERCIAL");
		db.setProfileStatus("APPROVED");
		when(tagMapper.recommend(eq("roy"), eq(10))).thenReturn(List.of(db));

		List<TagResponse> result = tagService.recommend(principal, "roy", 10);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getHashtag()).isEqualTo("royalcanin");
	}

	@Test
	void selectedPetIsLoadedFromItsOwnerAndWrongSpeciesResultsAreExcluded() {
		var pet=new me._hanho.ultary.domain.pet.model.Pet();
		pet.setName("별이");pet.setSpecies("CAT");pet.setBreed("코리안 숏헤어");
		when(petMapper.findActiveByPetIdAndUserNo(7L,101L)).thenReturn(pet);
		var dogProduct=new Tag();dogProduct.setHashtag("강아지하네스");dogProduct.setTitle("반려견 전용 하네스");
		when(tagMapper.recommend(null,100)).thenReturn(List.of(dogProduct));
		when(aiService.recommendHashtags(101L,"오늘 산책을 했어요",List.of(),List.of(new AiService.SelectedPet("별이","CAT","코리안 숏헤어"))))
			.thenReturn(List.of("강아지산책","고양이일상"));
		assertThat(tagService.recommend(new UserPrincipal(101L,"test"),"오늘 산책을 했어요",10,List.of(7L),null))
			.extracting(TagResponse::getHashtag).containsExactly("고양이일상");
	}

	@Test
	void rejectsForeignPetAndUsesOriginalAuthorsPetsForPrivilegedEdits() {
		var principal=new UserPrincipal(101L,"test");
		org.assertj.core.api.Assertions.assertThatThrownBy(()->tagService.recommend(principal,"산책",10,List.of(9L),null))
			.isInstanceOf(me._hanho.ultary.common.exception.BusinessException.class);
		when(feedService.getDetail(principal,22L)).thenReturn(me._hanho.ultary.domain.feed.dto.response.FeedResponse.builder().userNo(102L).build());
		org.assertj.core.api.Assertions.assertThatThrownBy(()->tagService.recommend(principal,"산책",10,List.of(9L),22L))
			.isInstanceOf(me._hanho.ultary.common.exception.BusinessException.class);
		var admin=new UserPrincipal(100L,"administrator");
		when(feedService.getDetail(admin,22L)).thenReturn(me._hanho.ultary.domain.feed.dto.response.FeedResponse.builder().userNo(102L).build());
		var pet=new me._hanho.ultary.domain.pet.model.Pet();pet.setSpecies("CAT");pet.setName("별이");
		when(petMapper.findActiveByPetIdAndUserNo(9L,102L)).thenReturn(pet);
		when(tagMapper.recommend(null,5)).thenReturn(List.of());
		assertThat(tagService.recommend(admin,"",5,List.of(9L),22L)).isEmpty();
		verify(petMapper).findActiveByPetIdAndUserNo(9L,102L);
	}

	@Test
	void recommend_blankQuery_usesDbOnly() {
		UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
		when(tagMapper.recommend(isNull(), eq(5))).thenReturn(List.of());

		assertThat(tagService.recommend(principal, "  ", 5)).isEmpty();
		verify(aiService, never()).recommendHashtags(org.mockito.ArgumentMatchers.any(), anyString());
	}

	@Test
	void recommendsSemanticDbCandidatesWithDescriptionsAndExcludesUnavailableTags() {
		Tag candidate = new Tag(); candidate.setTagId(3L); candidate.setHashtag("펫브러시");
		candidate.setPurpose("COMMERCIAL"); candidate.setProfileStatus("APPROVED");
		candidate.setTitle("슬리커 브러시"); candidate.setContent("털 관리용 브러시");
		when(tagMapper.recommend(null, 100)).thenReturn(List.of(candidate));
		var context = List.of(new AiService.ExistingHashtag("펫브러시", "슬리커 브러시", "털 관리용 브러시"));
		when(aiService.recommendHashtags(101L, "오늘 털을 빗겨줬어요", context))
			.thenReturn(List.of("털관리", "펫브러시", "대기태그", "거절태그"));
		when(tagMapper.findActiveByHashtag("펫브러시")).thenReturn(candidate);
		when(tagMapper.findActiveByHashtag("털관리")).thenReturn(null);
		Tag pending = new Tag(); pending.setPurpose("COMMERCIAL"); pending.setProfileStatus("PENDING");
		Tag rejected = new Tag(); rejected.setPurpose("USER"); rejected.setProfileStatus("REJECTED");
		when(tagMapper.findActiveByHashtag("대기태그")).thenReturn(pending);
		when(tagMapper.findActiveByHashtag("거절태그")).thenReturn(rejected);
		var result = tagService.recommend(new UserPrincipal(101L, "test"), "오늘 털을 빗겨줬어요", 10);
		assertThat(result).extracting(TagResponse::getHashtag).containsExactly("펫브러시", "털관리");
	}

	@Test
	void appliesLimitAfterDbPriorityAndDeduplicatesEnglishCase() {
		when(tagMapper.recommend(null, 100)).thenReturn(List.of());
		when(aiService.recommendHashtags(101L, "산책", List.of())).thenReturn(List.of("walk", "WALK", "산책"));
		Tag existing = new Tag(); existing.setTagId(1L); existing.setHashtag("산책"); existing.setPurpose("USER"); existing.setProfileStatus("NONE");
		when(tagMapper.findActiveByHashtag("산책")).thenReturn(existing);
		when(tagMapper.findActiveByHashtag("walk")).thenReturn(null);
		assertThat(tagService.recommend(new UserPrincipal(101L, "test"), "산책", 1))
			.extracting(TagResponse::getHashtag).containsExactly("산책");
	}
}
