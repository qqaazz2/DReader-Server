package io.github.qqaazz2.DReaderServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qqaazz2.DReaderServer.entity.LoggingEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoggingEventMapper extends BaseMapper<LoggingEvent> {
}
