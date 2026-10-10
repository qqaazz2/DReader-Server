package io.github.qqaazz2.DReaderServer.controller;

import io.github.qqaazz2.DReaderServer.common.ResultResponse;
import io.github.qqaazz2.DReaderServer.service.TagsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
