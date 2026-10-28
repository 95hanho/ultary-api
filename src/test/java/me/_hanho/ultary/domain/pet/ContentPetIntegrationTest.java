package me._hanho.ultary.domain.pet;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.dto.request.*;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.story.dto.request.CreateStoryRequest;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "ai.moderation.worker-enabled=false")
@Transactional
class ContentPetIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired FeedService feeds;
    @Autowired StoryService stories;
    @Autowired ContentPetService subjects;
    @Autowired PetMapper petMapper;
    Long owner, first, second, foreign, file;
    UserPrincipal principal;

    @BeforeEach void fixture() {
        owner = user(); first = pet(owner, "초코"); second = pet(owner, "Coco"); foreign = pet(user(), "달이");
        principal = new UserPrincipal(owner, "subject-test");
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('subject.jpg','images/subject.jpg','image/jpeg','jpg','OWNED',?)", owner);
        file = id();
    }

    @Test void feedCreationAndEditPersistOwnSubjectsSeparatelyFromPhotoTags() {
        var response = feeds.create(principal, feedRequest(List.of(first, second)));
        assertThat(response.getSubjectPets()).extracting(ContentPetService.SubjectPet::petId).containsExactly(first, second);
        assertThat(response.getPets()).isEmpty();
        var edit = new UpdateFeedRequest(); edit.setSubjectPetIds(List.of(second));
        assertThat(feeds.update(principal, response.getFeedId(), edit).getSubjectPets())
            .extracting(ContentPetService.SubjectPet::petId).containsExactly(second);
        edit.setSubjectPetIds(null);
        assertThat(feeds.update(principal, response.getFeedId(), edit).getSubjectPets()).hasSize(1);
        edit.setSubjectPetIds(List.of());
        assertThat(feeds.update(principal, response.getFeedId(), edit).getSubjectPets()).isEmpty();
    }

    @Test void storyCreationReturnsSelectedSubjectsAndOwnerRingMetadata() {
        var request = new CreateStoryRequest(); request.setFileId(file); request.setSubjectPetIds(List.of(second, first));
        var story = stories.create(principal, request);
        assertThat(story.getSubjectPets()).extracting(ContentPetService.SubjectPet::petId).containsExactly(second, first);
        assertThat(story.getMentions()).isEmpty();
        assertThat(stories.listResidentOwners(principal).get(0).getSubjectPets())
            .extracting(ContentPetService.SubjectPet::petId).containsExactly(first, second);
    }

    @Test void foreignOrDeletedPetsCannotReplaceAnExistingSelection() {
        var feed = feeds.create(principal, feedRequest(List.of(first)));
        assertThatThrownBy(() -> subjects.replace("FEED", feed.getFeedId(), owner, List.of(second, foreign)))
            .isInstanceOf(BusinessException.class);
        assertThat(subjects.list("FEED", feed.getFeedId())).extracting(ContentPetService.SubjectPet::petId).containsExactly(first);
        petMapper.softDelete(second, owner);
        assertThatThrownBy(() -> subjects.replace("FEED", feed.getFeedId(), owner, List.of(second)))
            .isInstanceOf(BusinessException.class);
    }

    CreateFeedRequest feedRequest(List<Long> selected) {
        var request = new CreateFeedRequest(); request.setSubjectPetIds(selected); request.setVisibility("PUBLIC");
        var media = new CreateFeedRequest.MediaItem(); media.setFileId(file); request.setMedia(List.of(media)); return request;
    }
    Long user() { jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "subject" + UUID.randomUUID().toString().substring(0, 12)); return id(); }
    Long pet(Long user, String name) { jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority) VALUES(?,?,?,1)", user, UUID.randomUUID().toString().replace("-", "").substring(0, 20), name); return id(); }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
