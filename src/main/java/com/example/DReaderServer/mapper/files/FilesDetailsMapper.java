package com.example.DReaderServer.mapper.files;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.DReaderServer.dto.book.SeriesListDTO;
import com.example.DReaderServer.dto.book.SeriesListQueryCondition;
import com.example.DReaderServer.dto.files.FilesDetailsItemDTO;
import com.example.DReaderServer.dto.files.FilesDetailsListDTO;
import com.example.DReaderServer.dto.files.FilesDetailsListQueryCondition;
import com.example.DReaderServer.dto.files.FilesOverviewListDTO;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.files.FilesDetails;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FilesDetailsMapper extends BaseMapper<FilesDetails> {
    List<FilesDetailsListDTO> getList(FilesDetailsListQueryCondition queryCondition);

    Integer count(FilesDetailsListQueryCondition queryCondition);

    FilesDetailsItemDTO getOne(Integer id);

    List<Integer> getStatusByParentId(int id);

    FilesDetailsListDTO getRecent();

    List<FilesOverviewListDTO> getOverview();
}
