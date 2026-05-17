package io.github.qqaazz2.DReaderServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qqaazz2.DReaderServer.dto.readLog.ReadLogDTO;
import io.github.qqaazz2.DReaderServer.dto.readLog.ReadLogStatisticsDTO;
import io.github.qqaazz2.DReaderServer.entity.ReadLog;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReadLogMapper extends BaseMapper<ReadLog> {
    List<ReadLogStatisticsDTO> statistics(LocalDateTime startTime, LocalDateTime endTime);

    List<ReadLogDTO> getReadLogListByTime(LocalDateTime startTime, LocalDateTime endTime);
}
