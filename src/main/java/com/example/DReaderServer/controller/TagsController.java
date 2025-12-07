package com.example.DReaderServer.controller;

import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.dto.logs.LogListQueryCondition;
import com.example.DReaderServer.service.LoggingEventService;
import com.example.DReaderServer.service.TagsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tags")
public class TagsController {
    @Resource
    TagsService tagsService;

    @GetMapping("/getTagsList")
    public ResultResponse getTagsList() {
        return ResultResponse.success(tagsService.getList());
    }
}
