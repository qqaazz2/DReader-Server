package com.example.DReaderServer.task.utils;

import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.service.files.FilesService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Data
public class ScanContext {
    private List<Files> filesList = new ArrayList<>(); //数据中存储的文件信息集
    private Map<Long, Files> folderByIdMap = new HashMap<>(); //数据中存储的文件信息集
    private Map<String, Files> filesByInodeMap = new HashMap<>(); //数据中存储的文件信息集
    private HashMap<String, Files> checkMap = new HashMap<>(); //获取到已经检测出来的文件信息
    private List<Files> createFiles = new ArrayList<>(); //需要新增的文件夹
    private List<Files> renameFiles = new ArrayList<>(); //需要重命名的文件及文件夹
    private List<Files> removeFiles = new ArrayList<>(); //需要删除的文件及文件夹
    private ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(20000));
    private File resourcesFile;

    public ScanContext(String resourcesPath, List<Files> filesList) {
        resourcesFile = new File(resourcesPath);
        if (resourcesFile.isFile()) throw new RuntimeException("扫描路径不是文件夹");
        this.filesList = filesList;
        folderByIdMap = filesList.stream().filter(item -> item.getIsFolder() == 1).collect(Collectors.toMap(Files::getId, item -> item));
        filesByInodeMap = filesList.stream().collect(Collectors.toMap(Files::getInode, item -> item));
    }
//    private List<CheckFileTask> list = new ArrayList<>();
//    List<String> skipFolder = new ArrayList<>(List.of("#recycle", "@eaDir", "@Recycle", "metaData.json"));
//    Map<String, List<Files>> createFilesMap = new HashMap<>();
//    Map<String, Integer> dbHasPathMap = new HashMap<>();//数据库中已有的文件路径值集合
//    static Map<Integer, List<Files>> parentChildrenMap = new HashMap<>();
//    List<Files> temporaryList = new ArrayList<>();
}
