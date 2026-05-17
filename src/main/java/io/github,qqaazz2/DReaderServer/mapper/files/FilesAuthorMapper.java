package io.github.qqaazz2.DReaderServer.mapper.files;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qqaazz2.DReaderServer.dto.files.FilesDetailsItemDTO;
import io.github.qqaazz2.DReaderServer.entity.files.FilesAuthor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FilesAuthorMapper extends BaseMapper<FilesAuthor> {
    List<FilesDetailsItemDTO.FilesDetailsAuthor> getAuthorByFilesId(Long id);
}
