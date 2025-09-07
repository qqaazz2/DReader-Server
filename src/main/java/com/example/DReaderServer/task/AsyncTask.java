package com.example.DReaderServer.task;

import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.common.TaskInterruptedException;
import com.example.DReaderServer.entity.Files;
import com.example.DReaderServer.entity.MetaData;
import com.example.DReaderServer.enums.FilesCheckType;
import com.example.DReaderServer.enums.FilesMatchType;
import com.example.DReaderServer.service.FilesService;
import com.example.DReaderServer.util.FileKeyAdapter;
import com.example.DReaderServer.util.FilesUtils;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Transactional
public abstract class AsyncTask {
    @Resource
    AsyncTaskExecutor taskExecutor;

    @Value("${file.upload}")
    String filePath;

    @Resource
    FilesUtils filesUtils;

    @Resource
    FilesService filesService;

    String resourcesPath = "";
    String basePath;
    File resourcesFile;
    int contentType;
    List<Files> filesList = new ArrayList<>(); //数据中存储的文件信息集
    HashMap<String, Files> checkMap = new HashMap<>(); //获取到已经检测出来的文件信息
    List<Files> createFiles = new ArrayList<>(); //需要新增的文件夹
    List<Files> renameFiles = new ArrayList<>(); //需要重命名的文件及文件夹
    ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(20000));
    List<CheckFileTask> list = new ArrayList<>();
    List<String> skipFolder = new ArrayList<>(List.of("#recycle", "@eaDir", "@Recycle", "metaData.json"));
    Map<String, List<Files>> createFilesMap = new HashMap<>();
    Map<String, Integer> dbHasPathMap = new HashMap<>();//数据库中已有的文件路径值集合
    static Map<Integer, List<Files>> parentChildrenMap = new HashMap<>();
    List<Files> temporaryList = new ArrayList<>();


    Map<String, Integer> folders = new HashMap<>();//文件夹的FilesID;
    private static final Object LOCK = new Object();

    public static ConcurrentHashMap<Class<?>, Thread> taskMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Class<?>, Integer> taskNumMap = new ConcurrentHashMap<>();

    public void startOrRestart(String path) {
        Class<?> taskClass = this.getClass();
        String taskName = Thread.currentThread().getName() + "-" + taskClass.getSimpleName();
        log.info("[{}] 新任务已启动", taskName);
        Thread oldTask = taskMap.get(taskClass);
        if (oldTask != null && oldTask.isAlive()) {
            log.info("发现旧任务[{}]正在运行，准备中止...", oldTask.getName() + "-" + taskClass.getSimpleName());
            oldTask.interrupt();
            try {
                oldTask.join();  // 延时100ms，减少CPU消耗，给旧线程响应时间
            } catch (InterruptedException e) {
                oldTask.interrupt();
                throw new TaskInterruptedException();
            }
            log.info("[{}]任务已完全终止", oldTask.getName() + "-" + taskClass.getSimpleName());
        }

        taskMap.put(taskClass, Thread.currentThread());
        try {
            start(path);
        } catch (TaskInterruptedException e) {
            log.info("[{}]任务被中断", taskName);
            throw e;
        } catch (Exception e) {
            log.error("[{}] 任务执行异常", taskName, e);
            throw e;
        } finally {
            taskMap.remove(taskClass);
        }
    }

    protected void start(String path) {
        checkMap.clear();
        renameFiles.clear();
        list.clear();
        createFilesMap.clear();
        createFiles.clear();
        parentChildrenMap.clear();
        //判断传入的文件夹路径是否为空
        if (path.equals("") || path.isEmpty()) {
            resourcesPath = basePath;
        } else {
            resourcesPath = path;
        }
        resourcesFile = new File(filePath + resourcesPath);

        if (resourcesFile.isFile()) throw new BizException("4000", "只能对文件夹扫描");
        executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(20000));
        filesList = filesService.getByType(contentType);
        setMapData();
        checkInterrupted();
        filesUtils.checkMetaFile(resourcesFile);
        createFiles = deepFolder(resourcesFile.listFiles(), 1, resourcesFile.getPath(), -1, resourcesFile.getPath());
        finish();
        checkInterrupted();
        log.info("扫描完成");
    }

    protected void setMapData() {
        int index = 0;
        for (Files files : filesList) {
            if (files.getIsFolder() == 2) {
                parentChildrenMap.computeIfAbsent(files.getParentId(), k -> new ArrayList<>()).add(files);
            } else if (files.getFilePath() != null) {
                dbHasPathMap.put(files.getFilePath(), index);
            }
            index++;
        }
    }

    protected List<Files> deepFolder(File[] files, Integer type, String parentPath, Integer currentFolderID, String mateDtaPath) {
        List<Files> filesList = new ArrayList<>();
        try {
            int index = 0;
            for (File file : files) {
                if (!skipFolder.isEmpty() && skipFolder.contains(file.getName())) continue;
                //判断文件是否为文件夹
                int deepType = type;
                if (file.isDirectory()) {
                    //判断文件夹中没有metadata 如果没有则为新创建的文件夹
                    //(所有没有的metadata文件的文件夹都会当作是新的文件夹，包括人为删除的)
                    if (!filesUtils.checkMetaFile(file)) {
                        Files files1 = filesUtils.createFolder(file, currentFolderID, contentType, file.list().length);
                        files1.setChild(deepFolder(file.listFiles(), deepType, parentPath + File.separator + file.getName(), -1, mateDtaPath + File.separator + file.getName()));
                        filesList.add(files1);
                        continue;
                    }

                    //获取文件夹中的metadata
                    MetaData metaData = filesUtils.checkFolderName(file);
                    String metaDataName = metaData.getName();
                    String mateDataPath = mateDtaPath + File.separator + metaDataName;
                    //如果metadata中的文件夹名字和拿到的文件夹名字不同则为重命名文件夹
                    //重命名的条件
                    //1.metadata里的数据和文件夹名称不相等
                    //2.数据库中存在metadata里的数据名称的值
                    String renamePath = parentPath;
                    String name = file.getName();
                    Integer pID = currentFolderID;
                    if (!metaDataName.equals(name) && checkDbData(file.getParent() + File.separator + metaDataName)) {
                        Files filesData = checkMap.get(file.getParent() + File.separator + metaDataName); //拿到checkList的最新一条数据
                        pID = filesData.getId();

                        //更新Files实体类
                        filesData.setFileName(file.getName());
                        filesData.setModifiableName(file.getName());
                        filesData.setFilePath(file.getPath()); //判断修改文件信息
                        renameFiles.add(filesData);
                        //更新Files实体类
                        renamePath = renamePath + File.separator + metaDataName;
                        deepType = 2;//设置type为重命名
                    } else if (!checkDbData(file.getPath())) {
                        if(!metaDataName.equals(name))filesUtils.editMetaData(file);
                        //这里对（有metadata文件并且没有重命名，但数据库中找不到文件信息）的操作
                        Files files1 = filesUtils.createFolder(file, currentFolderID, contentType, file.list().length - 1);
                        files1.setChild(deepFolder(file.listFiles(), deepType, parentPath + File.separator + file.getName(), -1, mateDataPath));
                        filesList.add(files1);
                        continue;
                    } else {
                        //这里是对没有重命名且文件信息再数据库中存在的信息的操作
                        checkDbData(file.getPath());
                        Files filesData = checkMap.get(file.getPath()); //拿到checkMap的最新一条数据
                        pID = filesData.getId();
                        renamePath = renamePath + File.separator + file.getName();
                    }
                     filesList.addAll(deepFolder(file.listFiles(), deepType, renamePath, pID, mateDataPath));
                } else if (file.isFile() && !filesUtils.isMetaFile(file)) {
                    boolean isTop = parentPath.equals(resourcesFile.getPath());
                    list.add(new CheckFileTask(file, currentFolderID, type, filesUtils, mateDtaPath, index,isTop));
                    index++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("扫描文件夹异常");
        }
        checkInterrupted();
        return filesList;
    }

    public void finish() {
        List<Future<FileTaskResult>> futures = new ArrayList<>();
        for (CheckFileTask checkFileTask : list) {
            checkInterrupted(() -> executor.shutdown());
            futures.add(executor.submit(checkFileTask));
        }
        executor.shutdown();

        try {
            boolean tasksCompleted = executor.awaitTermination(30, TimeUnit.SECONDS);
            while (!tasksCompleted) {
                tasksCompleted = executor.awaitTermination(30, TimeUnit.SECONDS);
            }
            String parentPath = "";
            for (Future<FileTaskResult> future : futures) {
                checkInterrupted();
                FileTaskResult fileTaskResult = future.get();
                Files files = fileTaskResult.getFiles();
                FilesCheckType checkType = fileTaskResult.getType();
                switch (checkType) {
                    case CREATE -> {
                        parentPath  = new File(files.getFilePath()).getParent();
                        createFilesMap.computeIfAbsent(parentPath, k -> new ArrayList<>()).add(files);
                    }
                    case RENAME -> {
                        renameFiles.add(files);
                        checkMap.put(files.getFilePath(), files);
                    }
                    case SPACIAL -> temporaryList.add(files);
                    case NORMAL -> checkMap.put(files.getFilePath(), files);
                }
            }

            Map<Integer, Map<String, List<Files>>> tempGrouped =
                    temporaryList.stream()
                            .collect(Collectors.groupingBy(
                                    Files::getParentId,
                                    Collectors.groupingBy(Files::getHash)
                            ));

            tempGrouped.forEach((parentId, hashMap) -> {
                List<Files> dbFiles = parentChildrenMap.getOrDefault(parentId, List.of());
                dbFiles.removeAll(checkMap.values());

                Map<String, List<Files>> dbFilesByHash =
                        dbFiles.stream().collect(Collectors.groupingBy(Files::getHash));

                hashMap.forEach((hash, tempFiles) -> {
                    List<Files> matchedDbFiles = dbFilesByHash.get(hash);

                    if (matchedDbFiles != null && matchedDbFiles.size() == 1 && tempFiles.size() == 1) {
                        Files dbFile = matchedDbFiles.get(0);
                        dbFile.setModifiableName(dbFile.getFileName());

                        renameFiles.add(dbFile);
                        checkMap.put(dbFile.getFilePath(), dbFile);
                    } else {
                        addToCreateFilesMap(tempFiles);
                    }
                });
            });


//            parentChildrenMap.forEach((key, item) -> {
//                item.removeAll(checkMap.values());
//                Map<String, List<Files>> unmatchedDbFilesByHash = item.stream().collect(Collectors.groupingBy(Files::getHash));
//                Map<String, List<Files>> temporaryMapByParentID = temporaryList.stream().filter(files -> files.getParentId().equals(key)).collect(Collectors.groupingBy(Files::getHash));
//                temporaryMapByParentID.forEach((k, v) -> {
//                    if (unmatchedDbFilesByHash.containsKey(k)) {
//                        if (unmatchedDbFilesByHash.get(k).size() == 1 && v.size() == 1) {
//                            Files files = unmatchedDbFilesByHash.get(k).get(0);
//                            files.setInode(files.getInode());
//                            files.setFileName(files.getFileName());
//                            files.setFilePath(files.getFilePath());
//                            files.setModifiableName(files.getFileName());
//                            renameFiles.add(files);
//                            checkMap.put(files.getFilePath(), files);
//                        } else {
//                            String path = "";
//                            for (Files files : v) {
//                                List<Files> list = new ArrayList<>();
//                                path = files.getFilePath().replaceAll(File.separator + files.getFileName(), "");
//                                if (createFilesMap.containsKey(path)) list = createFilesMap.get(path);
//                                list.add(files);
//                                createFilesMap.put(path, list);
//                            }
//                        }
//                    } else {
//                        String path = "";
//                        for (Files files : v) {
//                            List<Files> list = new ArrayList<>();
//                            path = files.getFilePath().replaceAll(File.separator + files.getFileName(), "");
//                            if (createFilesMap.containsKey(path)) list = createFilesMap.get(path);
//                            list.add(files);
//                            createFilesMap.put(path, list);
//                        }
//                    }
//                });
//            });

            //递归将文件（子Files）放到对应的文件夹（父Files）下
            createFileDeep(createFiles);
            createFilesMap.values().forEach(item -> createFiles.addAll(item));
            // 所有任务完成后继续执行后续操作
            if (!createFiles.isEmpty()) {
                create(); // 创建新的文件
            }
            rename(); // 重命名文件操作
            checkInterrupted();
            remove();
        } catch (InterruptedException e) {
            log.warn("等待子任务完成时被中断，开始清理...");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            checkInterrupted();
        } catch (TaskInterruptedException e) {
            log.warn("任务在非阻塞阶段被中断...");
            executor.shutdownNow(); // 同样需要清理
            checkInterrupted();
        } catch (Exception e) {
            e.printStackTrace();
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new BizException("4000", "文件扫描失败");
        }
    }

    private void addToCreateFilesMap(List<Files> filesList) {
        for (Files files : filesList) {
            String path = new File(files.getFilePath()).getParent();
            createFilesMap.computeIfAbsent(path, k -> new ArrayList<>()).add(files);
        }
    }

    private void createFileDeep(List<Files> list) {
        for (Files files : list) {
            if (files.getChild() != null) createFileDeep(files.getChild());

            if (createFilesMap.containsKey(files.getFilePath())) {
                List<Files> child = files.getChild();
                child.addAll(createFilesMap.get(files.getFilePath()));
                files.setChild(child);
                createFilesMap.remove(files.getFilePath());
            }
        }
        checkInterrupted();
    }

    protected boolean checkDbData(String checkPath) {
        if (dbHasPathMap.containsKey(checkPath)) {
            int index = dbHasPathMap.get(checkPath);
            checkMap.put(checkPath, filesList.get(index));
            return true;
        }
        return false;
    }

    public void rename() {
        checkInterrupted();
        filesService.renameFiles(renameFiles);
        renameFiles.stream().filter(value -> value.getIsFolder() == 1).forEach(value -> filesUtils.editMetaData(new File(value.getFilePath())));
    }

    public abstract void create();

    public void remove() {
        checkInterrupted();
        List<Files> filesL = checkMap.values().stream().toList();
        filesList.removeAll(filesL); //数据中存储的文件信息集和获取到已经检测出来的文件信息的差集
        filesService.removerFiles(filesList); //删除数据库中的数据
    }

    @PreDestroy
    public void onDestroy() {
        executor.shutdown();
    }

    protected void checkInterrupted() {
        boolean interrupted = Thread.currentThread().isInterrupted();
        if (interrupted) {
            throw new TaskInterruptedException();
        }
    }

    protected void checkInterrupted(Runnable onInterrupt) {
        boolean interrupted = Thread.currentThread().isInterrupted();
        if (interrupted) {
            try {
                onInterrupt.run();
            } catch (Exception e) {
                log.error("任务中断时执行清理逻辑失败", e);
            }
            Thread.currentThread().interrupt();
            throw new TaskInterruptedException();
        }
    }
}

@AllArgsConstructor
class CheckFileTask implements Callable<FileTaskResult> {
    private File file;
    private Integer parentID;
    private Integer type;
    private FilesUtils filesUtils;
    private String mateData;
    private int order;
    private boolean isTop;
//    private String parentPath;

    @Override
    public FileTaskResult call() throws Exception {
        //CREATE为新增 RENAME为重命名 NORMAL为正常（数据库中已经有数据不需要新增或重命名） SPACIAL暂时无法判断类型
        FilesCheckType filesType = FilesCheckType.NORMAL;
        Files files = null;
        if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
        String inode = FileKeyAdapter.getFileKey(file);
        String filePath = file.getPath();
        if (type == 1 && parentID == -1) {
            if (!isTop) {
                filesType = FilesCheckType.CREATE;
                files = filesUtils.createFiles(file, type, parentID, order, inode);
                return new FileTaskResult(filesType, files);
            }
        } else if (type == 2) {
            filesType = FilesCheckType.RENAME;
            filePath = mateData + File.separator + file.getName();
        }

        List<Files> list = AsyncTask.parentChildrenMap.get(parentID);
       if(list == null) {
           filesType = FilesCheckType.CREATE;
           files = filesUtils.createFiles(file, type, parentID, order, inode);
           return new FileTaskResult(filesType, files);
       }
        List<Files> filesList = list.stream().filter(item -> item.getInode().equals(inode)).collect(Collectors.toList());
        String fileHash = filesUtils.getFileChecksum(file);
        if (filesList.isEmpty()) {
            FilesMatchResult result = findSimilar(list, fileHash, filePath);
            files = result.getMatchedFile();
            filesType = FilesCheckType.RENAME;
            if ((result.type == FilesMatchType.EXACT) || (result.type == FilesMatchType.HASH && result.hashNum == 1)) {
                files.setInode(inode);
                files.setFileName(file.getName());
                files.setFilePath(file.getPath());
                files.setModifiableName(file.getName());
            } else if (result.type == FilesMatchType.HASH && result.hashNum > 1) {
                filesType = FilesCheckType.SPACIAL;
                files = filesUtils.createFiles(file, type, parentID, order, inode);
            } else {
                filesType = FilesCheckType.CREATE;
                files = filesUtils.createFiles(file, type, parentID, order, inode);
                return new FileTaskResult(filesType, files);
            }
        } else {
            files = filesList.get(0);
            if (!files.getHash().equals(fileHash)) {
                filesType = FilesCheckType.CREATE;
                files = filesUtils.createFiles(file, type, parentID, order, inode);
                return new FileTaskResult(filesType, files);
            }

            if (!files.getFilePath().equals(filePath)) {
                filesType = FilesCheckType.RENAME;
                files.setFileName(file.getName());
                files.setFilePath(file.getPath());
                files.setModifiableName(file.getName());
            }
        }

        if(type == 2) {
            filesType = FilesCheckType.RENAME;
            files.setFilePath(file.getPath());
        }
        return new FileTaskResult(filesType, files);
    }

    public static class FilesMatchResult {
        private FilesMatchType type;
        private Files matchedFile;
        private Integer hashNum;

        public FilesMatchResult(FilesMatchType exact, Files files, Integer hashNum) {
            this.type = exact;
            this.matchedFile = files;
            this.hashNum = hashNum;
        }

        public FilesMatchType getType() {
            return type;
        }

        public Files getMatchedFile() {
            return matchedFile;
        }
    }

    public FilesMatchResult findSimilar(List<Files> filesList, String hash, String filePath) {
        Files sameHash = null;
        Files samePath = null;
        Integer hashNum = 0;

        for (Files f : filesList) {
            boolean sameHashFlag = Objects.equals(f.getHash(), hash);
            boolean samePathFlag = Objects.equals(f.getFilePath(), filePath);

            if (sameHashFlag && samePathFlag) {
                return new FilesMatchResult(FilesMatchType.EXACT, f, hashNum);
            }

            if (samePathFlag && samePath == null) {
                samePath = f;
            }

            if (sameHashFlag) {
                hashNum++;
                if (sameHash == null) {
                    sameHash = f;
                }
            }
        }

        if (samePath != null) return new FilesMatchResult(FilesMatchType.PATH, samePath, hashNum);
        if (sameHash != null) return new FilesMatchResult(FilesMatchType.HASH, sameHash, hashNum);
        return new FilesMatchResult(FilesMatchType.NONE, null, hashNum);
    }
}

@Data
class FileTaskResult {
    FilesCheckType type;
    Files files;

    FileTaskResult(FilesCheckType type, Files files) {
        this.type = type;
        this.files = files;
    }
}