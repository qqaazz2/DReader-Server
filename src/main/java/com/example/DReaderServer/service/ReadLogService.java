package com.example.DReaderServer.service;

import com.example.DReaderServer.dto.readLog.ReadLogDTO;
import com.example.DReaderServer.dto.readLog.ReadLogStatisticsDTO;
import com.example.DReaderServer.entity.ReadLog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public interface ReadLogService {
    ReadLog saveReadLog(Integer readLogId);

    ReadLog startReadLog(Integer bookId);

    List<ReadLogStatisticsDTO> statisticsReadLog(String start,String end);

    List<ReadLogDTO> getReadLogListByTime(String time);
}
