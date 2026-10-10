package io.github.qqaazz2.DReaderServer.controller;

import io.github.qqaazz2.DReaderServer.common.ResultResponse;
import io.github.qqaazz2.DReaderServer.service.ReadLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/readLog")
public class ReadLogController {
    @Resource
    ReadLogService readLogService;

    @GetMapping("start")
    public ResultResponse start(@RequestParam Long filesId) {
        return ResultResponse.success(readLogService.startReadLog(filesId));
    }

    @GetMapping("record")
    public ResultResponse record(@RequestParam Integer readLogId) {
        return ResultResponse.success(readLogService.saveReadLog(readLogId));
    }

    @GetMapping("getReadLogListByTime")
    public ResultResponse getReadLogListByTime(@RequestParam String date) {
        return ResultResponse.success(readLogService.getReadLogListByTime(date));
    }

    @GetMapping("statisticsReadLog")
    public ResultResponse statisticsReadLog(@RequestParam String start,@RequestParam String end) {
        return ResultResponse.success(readLogService.statisticsReadLog(start,end));
    }
}
