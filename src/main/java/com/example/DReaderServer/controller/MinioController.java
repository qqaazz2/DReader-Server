package com.example.DReaderServer.controller;

import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.service.FilesService;
import com.example.DReaderServer.service.UploadService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/minio")
public class MinioController {
    @Resource
    UploadService uploadService;

    @GetMapping("/getObject")
    public ResultResponse getObject(String objectName) {
        return ResultResponse.success(uploadService.getObject(objectName));
    }
}
