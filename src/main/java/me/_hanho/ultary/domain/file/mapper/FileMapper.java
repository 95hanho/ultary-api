package me._hanho.ultary.domain.file.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.file.model.FileMeta;

@Mapper
public interface FileMapper {

	FileMeta findActiveByFileId(@Param("fileId") Long fileId);

	int insert(FileMeta fileMeta);

	int softDelete(@Param("fileId") Long fileId);
}
