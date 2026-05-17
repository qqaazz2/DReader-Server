package io.github.qqaazz2.DReaderServer.storage.impl;

import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
    public String uploadSplicing(byte[] data, String fileName, String contentType) {
       return upload(data,upload + fileName,contentType);
    }

    @Override
    public String upload(byte[] data, String fileName, String contentType) {
        fileName = fileName.replaceAll("\\\\", "/");
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
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
        objectName = objectName.replaceAll("\\\\", "/");
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

    @Override
    public Set<String> getFileList(String path) {
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(path + File.separator).recursive(false).build());
        Set<String> fileNames = new HashSet<>();
       try {
           for (Result<Item> result : results) {
               Item item = result.get();
               if(item.isDir()) continue;
               String objectName = item.objectName();
               String fileName = objectName.substring(objectName.lastIndexOf("/") + 1);
               fileNames.add(fileName);
           }
       }catch (Exception exception){
           throw new BizException("4000", "获取文件列表失败");
       }

       return fileNames;
    }

    @Override
    public void removeByList(Set<String> names) {
        List<DeleteObject> deletedObjects = names.stream().map(DeleteObject::new).collect(Collectors.toList());
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(bucket).objects(deletedObjects).build());
        List<DeleteError> errors = new ArrayList<>();
        for (Result<DeleteError> r : results) {
            try {
                errors.add(r.get());
            } catch (Exception e) {
                throw new BizException("4000", "MiniO批量删除失败");
            }
        }

        if (!errors.isEmpty()) throw new BizException("4000", "部分文件删除失败");
    }
}
