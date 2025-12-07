package com.example.DReaderServer.storage.impl;

import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.storage.FileAdapterService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;

@Service
public class MinioStorageServiceImpl extends FileAdapterService {

    @Value("${minio.bucket}")
    String bucket;

    @Autowired
    private MinioClient minioClient;

    @Value("${file.upload}")
    String upload;

    @Override
    public String upload(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            String contentType = Files.probeContentType(file.toPath());
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(upload + file.getPath())
                    .stream(inputStream, file.length(), -1)
                    .contentType(contentType) // 可选
                    .build();
            minioClient.putObject(putObjectArgs);

            return getUrl(file.getPath());
        } catch (Exception e) {
            throw new BizException("4000", "上传失败");
        }
    }

    @Override
    public String upload(byte[] data, String fileName, String contentType) {
        fileName = fileName.replaceAll("\\\\", "/");
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(upload + fileName)
                    .stream(inputStream, data.length, -1)
                    .contentType(contentType) // 可选
                    .build();
            minioClient.putObject(putObjectArgs);
            data = null;
        } catch (Exception e) {
            throw new BizException("4000", "上传失败");
        }
        return fileName;
    }

    @Override
    public String getUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) return null;

        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(objectName).expiry(300).method(Method.GET).build());
        } catch (Exception e) {
            throw new BizException("4000", "图片同步失败，请检查Minio服务状态");
        }
    }

    @Override
    public String getStorageType() {
        return "minio";
    }
}
