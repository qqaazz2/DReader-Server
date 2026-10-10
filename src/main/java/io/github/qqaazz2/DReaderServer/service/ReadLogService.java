package io.github.qqaazz2.DReaderServer.service;

import io.github.qqaazz2.DReaderServer.dto.readLog.ReadLogDTO;
import io.github.qqaazz2.DReaderServer.dto.readLog.ReadLogStatisticsDTO;
import io.github.qqaazz2.DReaderServer.entity.ReadLog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ReadLogService {
    ReadLog saveReadLog(Integer readLogId);

    ReadLog startReadLog(Long filesId);

    List<ReadLogStatisticsDTO> statisticsReadLog(String start,String end);

    List<ReadLogDTO> getReadLogListByTime(String time);
}
