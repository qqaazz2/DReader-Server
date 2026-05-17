package io.github.qqaazz2.DReaderServer.controller;

import io.github.qqaazz2.DReaderServer.common.ResultResponse;
import io.github.qqaazz2.DReaderServer.task.BookTask;
import io.github.qqaazz2.DReaderServer.task.CoverTask;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController {
    @Resource
    BookTask bookTask;

    @Resource
    CoverTask coverTask;

    @GetMapping("scanning")
    public ResultResponse scanning(@RequestParam(defaultValue = "", required = false) String path) {
        bookTask.startOrRestart(path);
        return ResultResponse.success();
    }

    @GetMapping("coverScanning")
    public ResultResponse coverScanning(@RequestParam(defaultValue = "", required = false) String path) {
        coverTask.startOrRestart();
        return ResultResponse.success();
    }
}
