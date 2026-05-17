package io.github.qqaazz2.DReaderServer.controller;

import io.github.qqaazz2.DReaderServer.common.ResultResponse;
import io.github.qqaazz2.DReaderServer.dto.logs.LogListQueryCondition;
import io.github.qqaazz2.DReaderServer.service.LoggingEventService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/log")
public class LogsController {
    @Resource
    LoggingEventService loggingEventService;

    @GetMapping("/getLogList")
    public ResultResponse getLogList(LogListQueryCondition condition) {
        return ResultResponse.success(loggingEventService.getLogList(condition));
    }
}
