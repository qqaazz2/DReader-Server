package com.example.DReaderServer.storage.impl;

import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.storage.FileAdapterService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
            File file = new File(upload + fileName);
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
}
