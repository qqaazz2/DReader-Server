package io.github.qqaazz2.DReaderServer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.dto.readLog.ReadLogDTO;
import io.github.qqaazz2.DReaderServer.dto.readLog.ReadLogStatisticsDTO;
import io.github.qqaazz2.DReaderServer.entity.ReadLog;
import io.github.qqaazz2.DReaderServer.mapper.ReadLogMapper;
import io.github.qqaazz2.DReaderServer.service.ReadLogService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReadLogServiceImpl extends ServiceImpl<ReadLogMapper, ReadLog> implements ReadLogService {

    @Resource
    ReadLogMapper readLogMapper;

    @Override
    public ReadLog saveReadLog(Integer readLogId) {
        ReadLog readLog = this.getById(readLogId);
        if (readLog == null) throw new BizException("4000", "阅读记录不存在");
        LocalDateTime dbTime = readLog.getTime();
        LocalDateTime now = LocalDateTime.now();
        int dbSeconds = readLog.getSeconds();
        long seconds = Duration.between(dbTime, now).getSeconds();
        if (!dbTime.toLocalDate().equals(now.toLocalDate())) {
            LocalDateTime endOfDbDay = dbTime.toLocalDate().atTime(LocalTime.MAX);
            long yesterdaySeconds = Duration.between(dbTime, endOfDbDay).getSeconds();
            readLog.setTime(endOfDbDay);
            readLog.setSeconds((int) yesterdaySeconds + dbSeconds);
            if (!this.updateById(readLog)) throw new BizException("4000", "保存昨日阅读记录失败");

            ReadLog newLog = new ReadLog();
            BeanUtils.copyProperties(readLog, newLog);
            newLog.setId(null);
            newLog.setTime(now);
            int todaySeconds = (int) Duration.between(now.toLocalDate().atStartOfDay(), now).getSeconds();
            newLog.setSeconds(todaySeconds);
            if (!this.save(newLog)) throw new BizException("4000", "保存今日阅读记录失败");
            return newLog;
        }

        readLog.setTime(now);
        readLog.setSeconds((int) seconds + dbSeconds);
        if (!this.updateById(readLog)) throw new BizException("4000", "保存阅读记录失败");
        return readLog;
    }

    @Override
    public ReadLog startReadLog(Long filesId) {
        LocalDate date = LocalDate.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        LambdaQueryWrapper<ReadLog> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(ReadLog::getFilesId, filesId);
        lambdaQueryWrapper.between(ReadLog::getTime, startOfDay, endOfDay);
        ReadLog readLog = this.getOne(lambdaQueryWrapper);

        if (readLog == null) {
            readLog = new ReadLog();
            readLog.setFilesId(filesId);
            readLog.setTime(LocalDateTime.now());
            readLog.setSeconds(0);
            if (!this.save(readLog)) throw new BizException("4000", "创建阅读记录失败");
        } else {
            readLog.setTime(LocalDateTime.now());
            this.updateById(readLog);
        }

        return readLog;
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<ReadLogStatisticsDTO> statisticsReadLog(String start, String end) {
        LocalDate startDate = LocalDate.parse(start, FORMATTER);
        LocalDate endDate = LocalDate.parse(end, FORMATTER);
        if (startDate.isAfter(endDate)) throw new BizException("4000", "阅读记录查询开始日不可大于结束日期");

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        List<ReadLogStatisticsDTO> list = readLogMapper.statistics(startTime, endTime);
        Map<String, ReadLogStatisticsDTO> map = list.stream().collect(Collectors.toMap(ReadLogStatisticsDTO::getDate, dto -> dto));

        List<ReadLogStatisticsDTO> finalList = new LinkedList<>();
        LocalDate localDate = startDate;
        while (!localDate.isAfter(endDate)){
            String date = localDate.format(FORMATTER);
            if(map.containsKey(date)){
                finalList.add(map.get(date));
            }else {
                finalList.add(new ReadLogStatisticsDTO(date,0));
            }
            localDate = localDate.plusDays(1);
        }
        return finalList;
    }

    @Override
    public List<ReadLogDTO> getReadLogListByTime(String time) {
        LocalDate date = LocalDate.parse(time, FORMATTER);
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);
        return readLogMapper.getReadLogListByTime(startTime, endTime);
    }
}
