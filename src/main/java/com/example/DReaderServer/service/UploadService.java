package com.example.DReaderServer.service;

import com.example.DReaderServer.common.BizException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.Resource;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Service
public class UploadService {

    @Value("${minio.bucket}")
    String bucket;

    @Autowired
    private MinioClient minioClient;

    public String upload(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            String contentType = Files.probeContentType(file.toPath());
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(file.getPath())
                    .stream(inputStream, file.length(), -1)
                    .contentType(contentType) // 可选
                    .build();
            minioClient.putObject(putObjectArgs);

            return getObject(file.getPath());
        } catch (Exception e) {
            throw new BizException("4000", "上传失败");
        }
    }

    public String upload(byte[] data, String fileName, String contentType) {
        fileName = fileName.replaceAll("\\\\", "/");
//        fileName = fileName.replaceFirst("/", "");
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(inputStream, data.length, -1)
                    .contentType(contentType) // 可选
                    .build();
            minioClient.putObject(putObjectArgs);
        } catch (Exception e) {
            throw new BizException("4000", "上传失败");
        }

        return fileName;
    }

    public String getObject(String objectName) {
        if (objectName == null || objectName.isBlank()) return null;

        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(objectName).expiry(300).method(Method.GET).build());
        } catch (Exception e) {
            throw new BizException("4000","图片同步失败，请检查Minio服务状态");
        }
    }

    private String generateUrl(String objectName) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(objectName).expiry(300).method(Method.GET).build());
            return url;
        } catch (Exception e) {
            throw new BizException("4000", "图片同步失败");
        }
    }
}
