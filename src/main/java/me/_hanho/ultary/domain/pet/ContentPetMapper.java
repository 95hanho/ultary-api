package me._hanho.ultary.domain.pet;

import java.util.List;
import org.apache.ibatis.annotations.*;
import me._hanho.ultary.domain.pet.model.Pet;

@Mapper
public interface ContentPetMapper {
    @Select("SELECT DISTINCT p.* FROM ultary_content_pet c JOIN ultary_story s ON c.content_type='STORY' AND c.content_id=s.story_id "
        + "JOIN ultary_pet p ON p.pet_id=c.pet_id AND p.user_no=s.user_no AND p.is_deleted=0 "
        + "WHERE s.user_no=#{owner} AND s.is_deleted=0 ORDER BY p.priority,p.pet_id")
    List<Pet> findStoryOwnerPets(@Param("owner") Long owner);
    @Delete("DELETE FROM ultary_content_pet WHERE content_type=#{type} AND content_id=#{id}")
    int delete(@Param("type") String type, @Param("id") Long id);

    @Insert("INSERT INTO ultary_content_pet(content_type,content_id,pet_id,sort_order) VALUES(#{type},#{id},#{petId},#{sort})")
    int insert(@Param("type") String type, @Param("id") Long id, @Param("petId") Long petId, @Param("sort") int sort);

    @Select("SELECT p.* FROM ultary_content_pet c JOIN ultary_pet p ON p.pet_id=c.pet_id AND p.is_deleted=0 "
        + "WHERE c.content_type=#{type} AND c.content_id=#{id} AND "
        + "((c.content_type='FEED' AND EXISTS(SELECT 1 FROM ultary_feed f WHERE f.feed_id=c.content_id AND f.user_no=p.user_no)) "
        + "OR (c.content_type='STORY' AND EXISTS(SELECT 1 FROM ultary_story s WHERE s.story_id=c.content_id AND s.user_no=p.user_no))) "
        + "ORDER BY c.sort_order,c.pet_id")
    List<Pet> find(@Param("type") String type, @Param("id") Long id);
}
