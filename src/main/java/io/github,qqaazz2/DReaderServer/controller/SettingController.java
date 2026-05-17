package io.github.qqaazz2.DReaderServer.controller;

import io.github.qqaazz2.DReaderServer.common.ResultResponse;
import io.github.qqaazz2.DReaderServer.service.files.FilesService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/setting")
public class SettingController {
    @Resource
    FilesService filesService;

    @GetMapping("proportion")
    public ResultResponse proportion() {
        return ResultResponse.success(filesService.filesProportion());
    }

    @GetMapping("count")
    public ResultResponse count() {
        return ResultResponse.success(filesService.getFilesCount());
    }

    @GetMapping("timeCount")
    public ResultResponse timeCount() {
        return ResultResponse.success(filesService.getYearMonth());
    }
}
