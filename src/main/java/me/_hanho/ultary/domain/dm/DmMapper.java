package me._hanho.ultary.domain.dm;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.dm.model.DmMessage;
import me._hanho.ultary.domain.dm.model.DmMessageRow;
import me._hanho.ultary.domain.dm.model.DmRoom;
import me._hanho.ultary.domain.dm.model.DmRoomRow;

@Mapper
public interface DmMapper {

	DmRoom findByPairKey(@Param("pairKey") String pairKey);

	DmRoom findById(@Param("dmRoomId") Long dmRoomId);

	int insertRoom(DmRoom room);

	int reopen(@Param("dmRoomId") Long dmRoomId, @Param("userNo") Long userNo);

	int leave(@Param("dmRoomId") Long dmRoomId, @Param("userNo") Long userNo);

	int markRead(
			@Param("dmRoomId") Long dmRoomId,
			@Param("userNo") Long userNo,
			@Param("messageId") Long messageId);

	int clearPeerLeft(@Param("dmRoomId") Long dmRoomId, @Param("senderUserNo") Long senderUserNo);

	int insertMessage(DmMessage message);

	Long findLatestMessageId(@Param("dmRoomId") Long dmRoomId);

	List<DmRoomRow> findRooms(@Param("userNo") Long userNo, @Param("limit") int limit);

	DmRoomRow findRoomRow(@Param("userNo") Long userNo, @Param("dmRoomId") Long dmRoomId);

	List<DmMessageRow> findMessages(
			@Param("dmRoomId") Long dmRoomId,
			@Param("beforeMessageId") Long beforeMessageId,
			@Param("limit") int limit);

	DmMessageRow findMessage(@Param("dmMessageId") Long dmMessageId);
}
