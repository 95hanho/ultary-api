package me._hanho.ultary.domain.myultary;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UltaryLinkMapper {
    record LinkRow(Long userNo, String nickname, String withdrawalStatus) {}
    @Select("SELECT l.target_user_no AS userNo,u.nickname,u.withdrawal_status AS withdrawalStatus FROM ultary_user_link l LEFT JOIN ultary_user u ON u.user_no=l.target_user_no WHERE l.owner_user_no=#{owner} ORDER BY l.sort_order,l.target_user_no")
    List<LinkRow> list(@Param("owner") Long owner);
    @Select("SELECT COALESCE(MAX(sort_order),0)+1 FROM ultary_user_link WHERE owner_user_no=#{owner}")
    int nextOrder(@Param("owner") Long owner);
    @Insert("INSERT IGNORE INTO ultary_user_link(owner_user_no,target_user_no,sort_order) VALUES(#{owner},#{target},#{sort})")
    int insert(@Param("owner") Long owner, @Param("target") Long target, @Param("sort") int sort);
    @Delete("DELETE FROM ultary_user_link WHERE owner_user_no=#{owner} AND target_user_no=#{target}")
    int delete(@Param("owner") Long owner, @Param("target") Long target);
}
