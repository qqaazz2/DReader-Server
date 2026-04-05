package com.example.DReaderServer.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.MetaData;
import com.example.DReaderServer.service.impl.files.FilesServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FilesUtils {
    @Value("${file.meta}")
    String metaName;
    @Resource
    FilesServiceImpl filesService;

    private static final Tika tika = new Tika();

    public Map<String, Files> createFile(List<File> fileList, Integer type) {
        try {
            Map<String, Files> map = new HashMap<>();
            List<Files> list = new ArrayList<>();
            for (File file : fileList) {
                if (!file.exists()) {
                    file.mkdirs();
                } else {
                    LambdaQueryWrapper<Files> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                    lambdaQueryWrapper.eq(Files::getFileName, file.getName());
                    Files files = filesService.getOne(lambdaQueryWrapper);
                    files.setFile(file);
                    map.put(files.getFileName(), files);
                }
                String inode = FileKeyAdapter.getFileKey(file);
                list.add(createFiles(file,  -1L, 0, inode));
            }
            filesService.saveBatch(list);
            map.putAll(list.stream().collect(Collectors.toMap(Files::getFileName, files -> files)));
            return map;
        } catch (Exception e) {
            throw new BizException("4000", "文件创建失败：" + e.getMessage());
        }
    }


    public Files createFiles(File file, Long parentId, Integer sort, String inode) {
        Files files = new Files();
        try {
            files.setFileName(file.getName());
            files.setFilePath(file.getPath());
            files.setFileType(tika.detect(file));
            files.setFile(file);
            files.setFileSize(file.length());
            files.setHash(this.getFileChecksum(file));
//            files.setModifiableName(file.getName());
            files.setParentId(parentId);
            files.setSort(sort);
            files.setInode(inode);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BizException("获取文件类型失败");
        }
        return files;
    }

    public Files createFolder(File file, Long parentId, Integer size,String customId) {
        try {
            Files files = new Files();
            files.setFileName(file.getName());
            files.setFilePath(file.getPath());
            files.setId(IdWorker.getId());
            files.setFileType("folder");
            files.setFile(file);
            files.setIsFolder(1);
            files.setInode(FileKeyAdapter.getFileKey(file));
            files.setHash(customId);
            files.setFileSize(size.longValue());
            files.setParentId(parentId);
            return files;
        } catch (IOException e) {
            e.printStackTrace();
            throw new BizException("4000", "创建元数据文件夹失败");
        }
    }

    public boolean checkMetaFile(String path) {
        ObjectMapper objectMapper = new ObjectMapper();
        MetaData metaData = new MetaData();
        Path paths = Paths.get(path + File.separator + metaName);
        boolean type = java.nio.file.Files.exists(paths);
        if (type) return true;
        return false;
    }

    public String createMetaFile(String path){
        ObjectMapper objectMapper = new ObjectMapper();
        MetaData metaData = new MetaData();
        Path paths = Paths.get(path + File.separator + metaName);
        try {
            if(!java.nio.file.Files.exists(paths)) java.nio.file.Files.createFile(paths);
            String timePrefix = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
            String uuid = timePrefix + "-" + UUID.randomUUID().toString();
            metaData.setId(uuid);
            objectMapper.writeValue(paths.toFile(), metaData);
            return uuid;
        } catch (IOException e) {
            e.printStackTrace();
            throw new BizException("4000", "创建元数据文件失败");
        }
    }


    public MetaData checkFolderId(String path) {
        ObjectMapper objectMapper = new ObjectMapper();
        MetaData metaData = new MetaData();
        File metaFile = new File(path + File.separator + metaName);
        try {
            metaData = objectMapper.readValue(metaFile, MetaData.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BizException("4000", "读取元数据文件信息失败");
        }
        return metaData;
    }

    public String getFileChecksum(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);

            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();

            byte[] bytes = digest.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BizException("40000", "计算文件Hash值有误");
        }
    }

    public static boolean isImageFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg") ||
                lowerCaseFileName.endsWith(".png") || lowerCaseFileName.endsWith(".gif") ||
                lowerCaseFileName.endsWith(".bmp") || lowerCaseFileName.endsWith(".tiff") ||
                lowerCaseFileName.endsWith(".webp");
    }

    public static boolean isWinSystem() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (os.contains("win")) return true;
        return false;
    }

    public boolean isMetaFile(File file) {
        if (file.getName().equals(metaName)) return true;
        return false;
    }

    public static Float getImgMp(int width, int height) {
        return (float) (width * height) / 1000000;
    }

    public void editMetaData(String path,String uuid) {
        Path paths = Paths.get(path + File.separator + metaName);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MetaData metaData = new MetaData();
            metaData.setId(uuid);
            objectMapper.writeValue(paths.toFile(), metaData);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BizException("4000", "编辑元数据文件失败");
        }
    }
}
