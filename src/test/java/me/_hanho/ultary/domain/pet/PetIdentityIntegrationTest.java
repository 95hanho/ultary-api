package me._hanho.ultary.domain.pet;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.main.MainService;
import me._hanho.ultary.domain.pet.model.Pet;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "ai.moderation.worker-enabled=false")
@Transactional
class PetIdentityIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PetService pets;
    @Autowired PetMapper mapper;
    @Autowired MainService main;
    @Autowired me._hanho.ultary.domain.dm.DmService dm;
    Long viewer, owner, first, second, file;
    String nickname;
    UserPrincipal principal;
    @BeforeEach void fixture() {
        viewer = user(); owner = user(); nickname = jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?", String.class, owner);
        principal = new UserPrincipal(viewer, "identity-test");
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('pet.jpg','images/pet.jpg','image/jpeg','jpg','OWNED',?)", owner); file = id();
        first = pet("초코", 1, null); second = pet("달이", 2, file);
    }
    @Test void representativeIsAlwaysTheFirstPetEvenWhenItsPhotoIsMissing() {
        assertThat(pets.representativeProfileFileId(owner)).isNull();
        assertThat(pets.representativeProfileFileIds(List.of(owner))).doesNotContainKey(owner);
        var result = main.search(principal, nickname, "USER", 10).getUsers().get(0);
        assertThat(result.getPetNames()).containsExactly("초코", "달이");
        assertThat(result.getProfileFile()).isNull();
        Pet patch = new Pet(); patch.setPetId(first); patch.setUserNo(owner); patch.setPriority(3); mapper.update(patch);
        assertThat(pets.representativeProfileFileId(owner)).isEqualTo(file.intValue());
        assertThat(pets.petNamesByUser(List.of(owner)).get(owner)).containsExactly("달이", "초코");
    }
    @Test void recentSearchUsesTheSameNamesAndFirstPetPortraitRule() {
        jdbc.update("INSERT INTO ultary_user_search_history(user_no,target_user_no) VALUES(?,?)", viewer, owner);
        var recent = main.getRecentSearches(principal).getItems().get(0);
        assertThat(recent.getPetNames()).containsExactly("초코", "달이");
        assertThat(recent.getPetTags()).hasSize(2);
        assertThat(recent.getProfileFile()).isNull();
    }
    @Test void accountAndRankedSearchAcceptDisplayedPetNamesAndNicknameTags() {
        assertThat(main.search(principal, "초코", "USER", 10).getUsers())
            .extracting(me._hanho.ultary.domain.main.dto.response.MainSearchUserItem::getUserNo).contains(owner);
        assertThat(main.search(principal, "@" + nickname, "MENTION", 10).getUsers())
            .extracting(me._hanho.ultary.domain.main.dto.response.MainSearchUserItem::getUserNo).contains(owner);
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status,accepted_at) VALUES(?,?,?,'ACCEPTED',NOW())", viewer, owner, viewer + ":" + owner);
        var target = dm.shareTargets(principal, 50, "초코", null, "DM").getItems().get(0);
        assertThat(target.getUserNo()).isEqualTo(owner);
        assertThat(target.getPetNames()).containsExactly("초코", "달이");
        assertThat(target.getPets()).extracting(me._hanho.ultary.domain.dm.dto.response.DmShareTargetItemResponse.PetChoice::name)
            .containsExactly("초코", "달이");
        assertThat(target.getProfileFile()).isNull();
    }
    @Test void retiredPetTagsCannotBeUsedAsSearchTerms() {
        String legacy=jdbc.queryForObject("SELECT mention_id FROM ultary_pet WHERE pet_id=?",String.class,first);
        assertThat(main.search(principal,legacy,"MENTION",10).getUsers()).isEmpty();
        assertThat(main.search(principal,legacy,"PET",10).getPets()).isEmpty();
    }
    Long user() { jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "id" + UUID.randomUUID().toString().substring(0, 8)); return id(); }
    Long pet(String name, int priority, Long photo) { jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority,profile_file_id) VALUES(?,?,?,?,?)", owner, UUID.randomUUID().toString().replace("-", "").substring(0, 20), name, priority, photo); return id(); }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
