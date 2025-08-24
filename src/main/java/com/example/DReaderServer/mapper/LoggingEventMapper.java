package com.example.DReaderServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.DReaderServer.entity.LoggingEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoggingEventMapper extends BaseMapper<LoggingEvent> {
}
