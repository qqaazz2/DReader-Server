package com.example.DReaderServer.common;

import com.example.DReaderServer.entity.Files;
import com.example.DReaderServer.util.FileTypeUtils;
import com.example.DReaderServer.util.FilesUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class UploadFile {
    @Value("${file.upload}")
    String filePath;

    @Resource
    FilesUtils filesUtils;

    public List<File> upload(MultipartFile[] files, String url, String[] formats) {
        File file = new File(filePath + url);
        if (!file.exists()) {
            file.mkdirs();
        }
        List<File> list = new ArrayList();
        for (MultipartFile multipartFile : files) {
            String originalFileName = multipartFile.getOriginalFilename();
            try {
                FileTypeUtils.validateFile(multipartFile, formats, 5000);
                String relativePath = file.getPath() + File.separator + originalFileName + ".jpg"; // 构建相对路径
                File createFile = new File(file.getAbsolutePath() + File.separator + originalFileName + ".jpg"); // 使用 filePath + 相对路径创建 File 对象
                multipartFile.transferTo(createFile);
                list.add(new File(relativePath)); // 添加相对路径的 File 对象到列表
            } catch (Exception e) {
                e.printStackTrace();
                throw new BizException("4004", "文件上传失败");
            }
        }
        return list;
    }

    public Map<String, Files> uploadFile(MultipartFile[] files, String url, String[] formats, Integer type) {
        File file = new File(filePath + url);
        List<File> list = new ArrayList();
        for (MultipartFile multipartFile : files) {
            String originalFileName = multipartFile.getOriginalFilename();
            try {
                FileTypeUtils.validateFile(multipartFile, formats,5000);
                File createFile = new File(file.getAbsolutePath() + File.separator + multipartFile.getOriginalFilename());
                list.add(createFile);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BizException("4004", "文件上传失败");
            }
        }

        return filesUtils.createFile(list, type);
    }


    private boolean checkFormats(String fileFullName, String[] formats) {
        String suffix = fileFullName.substring(fileFullName.lastIndexOf(".") + 1).toLowerCase();
        return Arrays.stream(formats).anyMatch(suffix::contains);
    }
}
