package me._hanho.ultary.domain.file;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.file.model.FileMeta;

@Mapper
public interface FileMapper {

	FileMeta findActiveByFileId(@Param("fileId") Long fileId);

	List<FileMeta> findActiveByFileIds(@Param("fileIds") Collection<Long> fileIds);

	int insert(FileMeta fileMeta);

	int softDelete(@Param("fileId") Long fileId);

	/** 활성 펫 프로필, 피드 미디어, 스토리, 태그 이미지에 남은 참조 수 */
	int countLiveReferences(@Param("fileId") Long fileId);
}
