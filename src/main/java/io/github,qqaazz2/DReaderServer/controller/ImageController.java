package io.github.qqaazz2.DReaderServer.controller;

import io.github.qqaazz2.DReaderServer.common.ResultResponse;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterFactory;
import io.github.qqaazz2.DReaderServer.storage.impl.SystemStorageServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/image")
public class ImageController {
    @Value("${token.secret}")
    String secret;

    @Resource
    FileAdapterFactory adapterFactory;

    @GetMapping("/getObject")
    public ResultResponse getObject(String objectName) {
        return ResultResponse.success(adapterFactory.getFileAdapter().getUrl(objectName));
    }

    @GetMapping("/getCurrentAdapter")
    public ResultResponse getCurrentAdapter() {
        return ResultResponse.success(adapterFactory.getFileAdapter().getStorageType());
    }

    @GetMapping("/getAdapterList")
    public ResultResponse getAdapterList() {
        return ResultResponse.success(adapterFactory.getAdapterTypeList());
    }

    @GetMapping("/system/view")
    public void systemFileView(@RequestParam String path, @RequestParam String sign, @RequestParam long expiration, HttpServletResponse response) {
        if(System.currentTimeMillis() > expiration){
            response.setStatus(403);
            return;
        }

        boolean success = SystemStorageServiceImpl.generatedSignText(path,secret,expiration).equals(sign);
        if(!success){
            response.setStatus(403);
            return;
        }

        File file = new File(path);
        if(!file.exists()){
            response.setStatus(404);
            return;
        }

        response.setContentType("image/jpeg");
        try {
            Files.copy(Path.of(file.getPath()),response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
