package io.github.qqaazz2.DReaderServer.mapper.files;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qqaazz2.DReaderServer.dto.files.*;
import io.github.qqaazz2.DReaderServer.dto.files.*;
import io.github.qqaazz2.DReaderServer.entity.files.FilesDetails;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FilesDetailsMapper extends BaseMapper<FilesDetails> {
    List<FilesDetailsListDTO> getList(FilesDetailsListQueryCondition queryCondition);

    Integer count(FilesDetailsListQueryCondition queryCondition);

    FilesDetailsItemDTO getOne(Long fileId);

    List<Integer> getStatusByParentId(Long id);

    FilesDetailsListDTO getRecent();

    List<FilesCoverChangeDTO> randomValidFiles();

    FilesDetailsListDTO randomData(Long filesId);

    List<FilesOverviewListDTO> getOverview();

    List<FilesCoverChangeDTO> getCoverList();
}
