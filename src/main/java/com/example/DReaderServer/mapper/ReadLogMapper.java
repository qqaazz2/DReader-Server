package com.example.DReaderServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.DReaderServer.dto.readLog.ReadLogDTO;
import com.example.DReaderServer.dto.readLog.ReadLogStatisticsDTO;
import com.example.DReaderServer.entity.ReadLog;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReadLogMapper extends BaseMapper<ReadLog> {
    List<ReadLogStatisticsDTO> statistics(LocalDateTime startTime, LocalDateTime endTime);

    List<ReadLogDTO> getReadLogListByTime(LocalDateTime startTime, LocalDateTime endTime);
}
