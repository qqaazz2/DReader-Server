package com.example.DReaderServer.controller;

import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.dto.QueryCondition;
import com.example.DReaderServer.service.LoggingEventService;
import com.example.DReaderServer.dto.logs.LogListQueryCondition;
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
