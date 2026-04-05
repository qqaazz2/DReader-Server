package com.example.DReaderServer.mapper.files;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.DReaderServer.dto.files.FilesDetailsItemDTO;
import com.example.DReaderServer.entity.files.FilesAuthor;
import com.example.DReaderServer.entity.files.FilesTags;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FilesTagsMapper extends BaseMapper<FilesTags> {
    List<FilesDetailsItemDTO.FilesDetailsTag> getTagsByFilesId(Long id);
}
