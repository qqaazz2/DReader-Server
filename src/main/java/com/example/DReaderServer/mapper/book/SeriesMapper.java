package com.example.DReaderServer.mapper.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.DReaderServer.dto.QueryCondition;
import com.example.DReaderServer.dto.book.SeriesListDTO;
import com.example.DReaderServer.dto.book.SeriesListQueryCondition;
import com.example.DReaderServer.entity.book.Series;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SeriesMapper extends BaseMapper<Series> {
    List<SeriesListDTO> getList(SeriesListQueryCondition queryCondition);

    Integer count(SeriesListQueryCondition queryCondition);

    SeriesListDTO getOne(Integer id);
}
