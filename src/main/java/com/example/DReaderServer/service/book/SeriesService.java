package com.example.DReaderServer.service.book;

import com.example.DReaderServer.dto.PageVO;
import com.example.DReaderServer.dto.book.SeriesListDTO;
import com.example.DReaderServer.dto.book.SeriesListQueryCondition;
import com.example.DReaderServer.entity.book.Series;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public interface SeriesService {
    List<Series> createData(List<Series> list);

    void updateNum(Integer filesId,Integer num);

    PageVO<SeriesListDTO> getList(SeriesListQueryCondition seriesListQueryCondition);

    void updateLove(Integer id,Integer love);

    void updateStatus(Integer id,Integer status);

    Series updateData(Series series);

    SeriesListDTO getDetails(Integer id);

    Integer getIdByFilesId(Integer id);

    void updateCover(Integer id,String cover);

    Date updateLastReadTime(Integer id);

    Date updateLastReadTimeByFilesId(Integer id);

    SeriesListDTO randomData();

}
