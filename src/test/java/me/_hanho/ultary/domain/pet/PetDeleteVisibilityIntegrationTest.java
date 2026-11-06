package me._hanho.ultary.domain.pet;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.myultary.MyUltaryService;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.story.dto.request.CreateStoryRequest;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "ai.moderation.worker-enabled=false")
@Transactional
class PetDeleteVisibilityIntegrationTest {
	@Autowired JdbcTemplate jdbc;
	@Autowired SqlSessionTemplate sqlSession;
	@Autowired FeedService feeds;
	@Autowired StoryService stories;
	@Autowired PetService pets;
	@Autowired MyUltaryService myUltary;
	Long owner, soloPet, sharedPet, file;
	UserPrincipal principal;

	@BeforeEach
	void fixture() {
		owner = user();
		soloPet = pet(owner, 1);
		sharedPet = pet(owner, 2);
		principal = new UserPrincipal(owner, "pet-del");
		jdbc.update(
				"INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('del.jpg','images/del.jpg','image/jpeg','jpg','OWNED',?)",
				owner);
		file = id();
	}

	@Test
	void soloFeedAndStoryBecomeNotFoundAfterPetDeleteAndReturnAfterRestore() {
		var soloFeed = feeds.create(principal, feedRequest(List.of(soloPet)));
		var sharedFeed = feeds.create(principal, feedRequest(List.of(soloPet, sharedPet)));
		var soloStory = stories.create(principal, storyRequest(List.of(soloPet)));
		var sharedStory = stories.create(principal, storyRequest(List.of(soloPet, sharedPet)));

		pets.delete(principal, soloPet);

		assertThatThrownBy(() -> feeds.getDetail(principal, soloFeed.getFeedId()))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FEED_NOT_FOUND));
		assertThatThrownBy(() -> stories.markViewed(principal, soloStory.getStoryId()))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STORY_NOT_FOUND));
		assertThat(stories.listByUser(principal, owner)).extracting(s -> s.getStoryId())
				.contains(sharedStory.getStoryId())
				.doesNotContain(soloStory.getStoryId());

		var sharedFeedView = feeds.getDetail(principal, sharedFeed.getFeedId());
		assertThat(sharedFeedView.getSubjectPets()).extracting(ContentPetService.SubjectPet::petId).containsExactly(sharedPet);
		assertThat(sharedFeedView.getPets()).extracting(p -> p.getPetId()).doesNotContain(soloPet);

		assertThat(stories.listByUser(principal, owner).stream()
				.filter(s -> s.getStoryId().equals(sharedStory.getStoryId())).findFirst().orElseThrow().getSubjectPets())
				.extracting(ContentPetService.SubjectPet::petId).containsExactly(sharedPet);

		assertThat(myUltary.getFeeds(principal, 50)).extracting(i -> i.getFeedId())
				.contains(sharedFeed.getFeedId())
				.doesNotContain(soloFeed.getFeedId());

		jdbc.update("UPDATE ultary_pet SET is_deleted=0, deleted_at=NULL WHERE pet_id=?", soloPet);
		sqlSession.clearCache();

		assertThat(feeds.getDetail(principal, soloFeed.getFeedId()).getSubjectPets())
				.extracting(ContentPetService.SubjectPet::petId).containsExactly(soloPet);
		assertThat(stories.listByUser(principal, owner)).extracting(s -> s.getStoryId()).contains(soloStory.getStoryId());
	}

	@Test
	void photoMentionOnlyCountsAsSoloSelectionForVisibility() {
		var created = feeds.create(principal, feedRequest(List.of()));
		Long mediaId = jdbc.queryForObject(
				"SELECT feed_media_id FROM ultary_feed_media WHERE feed_id=? ORDER BY sort_order LIMIT 1",
				Long.class, created.getFeedId());
		jdbc.update(
				"INSERT INTO ultary_feed_media_mention(feed_media_id,pet_id,pos_x,pos_y,added_by_user_no) VALUES(?,?,?,?,?)",
				mediaId, soloPet, new BigDecimal("10.00"), new BigDecimal("20.00"), owner);

		pets.delete(principal, soloPet);

		assertThatThrownBy(() -> feeds.getDetail(principal, created.getFeedId()))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FEED_NOT_FOUND));
	}

	@Test
	void subjectOnlyDeletedHidesEvenWhenTaggedPetsRemain() {
		Long tagOwner = user();
		Long taggedPet = pet(tagOwner, 1);
		var created = feeds.create(principal, feedRequest(List.of(soloPet)));
		Long mediaId = jdbc.queryForObject(
				"SELECT feed_media_id FROM ultary_feed_media WHERE feed_id=? ORDER BY sort_order LIMIT 1",
				Long.class, created.getFeedId());
		jdbc.update(
				"INSERT INTO ultary_feed_pet(feed_id,pet_id,added_by_user_no,role,is_main) VALUES(?,?,?,'COLLABORATOR',0)",
				created.getFeedId(), taggedPet, owner);
		jdbc.update(
				"INSERT INTO ultary_feed_media_mention(feed_media_id,pet_id,pos_x,pos_y,added_by_user_no) VALUES(?,?,?,?,?)",
				mediaId, taggedPet, new BigDecimal("10.00"), new BigDecimal("20.00"), owner);

		pets.delete(principal, soloPet);

		assertThatThrownBy(() -> feeds.getDetail(principal, created.getFeedId()))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FEED_NOT_FOUND));
		assertThat(myUltary.getFeeds(principal, 50)).extracting(i -> i.getFeedId())
				.doesNotContain(created.getFeedId());
	}

	CreateFeedRequest feedRequest(List<Long> subjects) {
		var request = new CreateFeedRequest();
		request.setSubjectPetIds(subjects);
		request.setVisibility("PUBLIC");
		var media = new CreateFeedRequest.MediaItem();
		media.setFileId(file);
		request.setMedia(List.of(media));
		return request;
	}

	CreateStoryRequest storyRequest(List<Long> subjects) {
		var request = new CreateStoryRequest();
		request.setFileId(file);
		request.setSubjectPetIds(subjects);
		return request;
	}

	Long user() {
		jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "pdel" + UUID.randomUUID().toString().substring(0, 12));
		return id();
	}

	Long pet(Long user, int priority) {
		jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority) VALUES(?,?,?,?)",
				user, UUID.randomUUID().toString().replace("-", "").substring(0, 20), "pet" + priority, priority);
		return id();
	}

	Long id() {
		return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}
}
