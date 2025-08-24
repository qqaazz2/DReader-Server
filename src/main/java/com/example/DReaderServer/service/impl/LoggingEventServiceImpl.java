package com.example.DReaderServer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.DReaderServer.dto.logs.LogDTO;
import com.example.DReaderServer.dto.PageVO;
import com.example.DReaderServer.dto.QueryCondition;
import com.example.DReaderServer.dto.logs.LogListQueryCondition;
import com.example.DReaderServer.entity.LoggingEvent;
import com.example.DReaderServer.mapper.LoggingEventMapper;
import com.example.DReaderServer.service.LoggingEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoggingEventServiceImpl extends ServiceImpl<LoggingEventMapper, LoggingEvent> implements LoggingEventService {
    public PageVO<LogDTO> getLogList(LogListQueryCondition condition) {
        LambdaQueryWrapper<LoggingEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(condition.getLevels() != null && !condition.getLevels().isEmpty(), LoggingEvent::getLevelString, condition.getLevels());
        queryWrapper.orderByDesc(LoggingEvent::getEventId);
        List<LoggingEvent> list = this.page(new Page<>(condition.getPage(), condition.getLimit()), queryWrapper).getRecords();
        Long count = this.count(queryWrapper);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<LogDTO> logDTOList = list.stream().map(item -> {
            LocalDateTime localDateTime = Instant.ofEpochMilli(item.getTimestmp()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            String time = localDateTime.format(formatter);
            LogDTO logDTO = new LogDTO(item.getEventId().intValue(), time, item.getFormattedMessage(), item.getLevelString(), item.getCallerFilename());
            return logDTO;
        }).collect(Collectors.toList());

        return new PageVO<LogDTO>(condition.getLimit(), condition.getPage(), count.intValue(), logDTOList);
    }
}