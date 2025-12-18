package com.example.DReaderServer.storage.impl;

import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.storage.FileAdapterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SystemStorageServiceImpl extends FileAdapterService {
    @Value("${file.upload}")
    String upload;

    @Value("${token.secret}")
    String secret;

    @Override
    public String upload(File file) {
        try {
            File uploadFile = new File(upload + file.getName());
            if (!uploadFile.exists()) uploadFile.mkdirs();
            Files.copy(file.toPath(), uploadFile.toPath());
            return uploadFile.getPath();
        } catch (Exception exception) {
            throw new BizException("4000", "文件创建失败");
        }
    }

    @Override
    public String upload(byte[] data, String fileName, String contentType) {
        try {
            File file = new File(fileName);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            Files.write(Paths.get(file.getPath()), data);
            return file.getPath();
        } catch (IOException e) {
            e.printStackTrace();
            throw new BizException("4000", "文件创建失败");
        }
    }

    @Override
    public String uploadSplicing(byte[] data, String fileName, String contentType) {
        return upload(data,upload + fileName,contentType);
    }

    @Override
    public String getUrl(String filePath) {
        String currentBaseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        long expiration = System.currentTimeMillis() + 300000;
        String singText = generatedSignText(filePath, secret, expiration);
        return String.format("%s/image/system/view?path=%s&sign=%s&expiration=%s", currentBaseUrl, filePath, singText, expiration);
    }

    public static String generatedSignText(String filePath, String secret, long expiration) {
        String text = "signText:" + filePath + secret + expiration;
        return DigestUtils.md5Hex(text);
    }

    @Override
    public String getStorageType() {
        return "system";
    }

    @Override
    public Set<String> getFileList(String path) {
        File file = new File(path);
        if (!file.exists()) file.mkdirs();
        return Arrays.stream(file.list()).toList().stream().collect(Collectors.toSet());
    }

    @Override
    public void removeByList(Set<String> names) {
        Set<String> failed = new HashSet<>();
        names.parallelStream().forEach(name -> {
            try {
                Files.deleteIfExists(Paths.get(name));
            } catch (IOException e) {
                failed.add(name);
            }
        });

        if(!failed.isEmpty()) log.error("4000", "部分文件删除失败");
    }
}
