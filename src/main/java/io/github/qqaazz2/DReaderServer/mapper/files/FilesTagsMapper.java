package io.github.qqaazz2.DReaderServer.mapper.files;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qqaazz2.DReaderServer.dto.files.FilesDetailsItemDTO;
import io.github.qqaazz2.DReaderServer.entity.files.FilesTags;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FilesTagsMapper extends BaseMapper<FilesTags> {
    List<FilesDetailsItemDTO.FilesDetailsTag> getTagsByFilesId(Long id);
}
