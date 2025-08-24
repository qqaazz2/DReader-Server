package com.example.DReaderServer.task;

import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.common.TaskInterruptedException;
import com.example.DReaderServer.entity.Files;
import com.example.DReaderServer.entity.MetaData;
import com.example.DReaderServer.service.FilesService;
import com.example.DReaderServer.util.FilesUtils;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    Files createData = new Files(); //需要新增的文件夹
    List<Files> renameFiles = new ArrayList<>(); //需要重命名的文件及文件夹
    ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(20000));
    List<CheckFileTask> list = new ArrayList<>();
    public static Files coverFiles = new Files();
    List<String> skipFolder = new ArrayList<>(List.of("#recycle", "@eaDir", "@Recycle","metaData.json"));
    Map<String, List<Files>> createFilesMap = new HashMap<>();

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
        checkInterrupted();
        filesUtils.checkMetaFile(resourcesFile);
        createFiles = deepFolder(resourcesFile.listFiles(), 1, resourcesFile.getPath(), -1);
        finish();
        checkInterrupted();
        log.info("扫描完成");
    }

    protected List<Files> deepFolder(File[] files, Integer type, String parentPath, Integer currentFolderID) {
        List<Files> filesList = new ArrayList<>();
        int index = 0;
        for (File file : files) {
            if (!skipFolder.isEmpty() && skipFolder.contains(file.getName())) continue;
            //判断文件是否为文件夹
            if (file.isDirectory()) {
                //判断文件夹中没有metadata 如果没有则为新创建的文件夹
                //(所有没有的metadata文件的文件夹都会当作是新的文件夹，包括人为删除的)
                if (!filesUtils.checkMetaFile(file)) {
                    Files files1 = filesUtils.createFolder(file, currentFolderID, contentType, file.list().length);
                    files1.setChild(deepFolder(file.listFiles(), type, parentPath + File.separator + file.getName(), currentFolderID));
                    filesList.add(files1);
                    continue;
                }

                //获取文件夹中的metadata
                MetaData metaData = filesUtils.checkFolderName(file);
                String metaDataName = metaData.getName();

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
                    type = 2;//设置type为重命名
                } else if (metaDataName.equals(file.getName()) && !checkDbData(file.getPath())) {
                    //这里对（有metadata文件并且没有重命名，但数据库中找不到文件信息）的操作
                    System.out.println("currentFolderID" + currentFolderID);
                    Files files1 = filesUtils.createFolder(file, currentFolderID, contentType, file.list().length - 1);
                    files1.setChild(deepFolder(file.listFiles(), type, parentPath + File.separator + file.getName(), currentFolderID));
                    filesList.add(files1);
                    continue;
                } else {
                    //这里是对没有重命名且文件信息再数据库中存在的信息的操作
                    checkDbData(file.getPath());
                    Files filesData = checkMap.get(file.getPath()); //拿到checkList的最新一条数据
                    pID = filesData.getId();
                    renamePath = renamePath + File.separator + file.getName();
                }
                deepFolder(file.listFiles(), type, renamePath, pID);
            } else if (file.isFile() && !filesUtils.isMetaFile(file)) {
                Files fliesData = null;
                if (checkDbData(file.getPath())) fliesData = checkMap.get(file.getPath());
                list.add(new CheckFileTask(file, parentPath, currentFolderID, type, filesUtils, contentType, index, fliesData));
                index++;
            }
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

            for (Future<FileTaskResult> future : futures) {
                checkInterrupted();
                FileTaskResult fileTaskResult = future.get();
                String parentPath = fileTaskResult.getParentPath();
                Files files = fileTaskResult.getFiles();
                if (fileTaskResult.getType() == 2) {
                    renameFiles.add(fileTaskResult.getFiles());
                    continue;
                }

                List<Files> list = new ArrayList<>();
                if (createFilesMap.containsKey(parentPath)) list = createFilesMap.get(parentPath);
                list.add(files);
                createFilesMap.put(parentPath, list);
            }
            //递归将文件（子Files）放到对应的文件夹（父Files）下
            createFileDeep(createFiles);
            // 所有任务完成后继续执行后续操作
            rename(); // 重命名文件操作
            if (!createFiles.isEmpty()) {
                create(); // 创建新的文件
            }
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

    private void createFileDeep(List<Files> list) {
        for (Files files : list) {
            if (files.getChild() != null) createFileDeep(files.getChild());

            if (createFilesMap.containsKey(files.getFilePath())) {
                List<Files> child = files.getChild();
                child.addAll(createFilesMap.get(files.getFilePath()));
                files.setChild(child);
            }
        }
        checkInterrupted();
    }

    protected boolean checkDbData(String checkPath) {
        List<Files> files = filesList.stream().filter(item -> item.getFilePath().equals(checkPath)).collect(Collectors.toList());
        if (files.isEmpty()) return false; //数据库中不存在这个文件的信息
        checkMap.put(checkPath, files.get(0));
        return true;
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


    public void getChildren(List<Files> files) {
        folders.putAll(files.stream().collect(Collectors.toMap(Files::getFilePath, Files::getId)));
        createFiles.stream().filter(value -> value.getIsFolder() == 2).forEach(value -> {
            value.setParentId(folders.get(value.getFile().getParent()));
        });
        createFiles.removeIf(value -> value.getIsFolder() == 1);
    }

    @PreDestroy
    public void onDestroy() {
        executor.shutdown();
    }

    public void createCover() {
        File file = new File(filePath + resourcesPath + File.separator + "cover");
        Files files = new Files();
        files.setFileName("cover");
        files.setParentId(-1);
        files.setType(contentType);
        files.setIsFolder(1);
        coverFiles = filesService.getFiles(files);

        if (!file.exists()) {
            file.mkdirs();
            filesUtils.checkMetaFile(file);
        }

        if (coverFiles == null) {
            coverFiles = filesUtils.createFolder(file, -1, contentType, 0);
            coverFiles = filesService.createFile(coverFiles);
        }
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
    private String parentPath;
    private Integer currentFolderID;
    private Integer type;
    private FilesUtils filesUtils;
    private Integer contentType;
    private int order;
    private Files fliesData;

    @Override
    public FileTaskResult call() throws Exception {
        if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
        short filesType = 1;
        //判断这个文件是否数据库中
        String renamePath = type == 2 ? parentPath + File.separator + file.getName() : file.getPath();
        if (fliesData != null) {
            String fileHash = filesUtils.getFileChecksum(file);
            //判断文件hash是否相同 不相同则为新文件
            if (!fliesData.getHash().equals(fileHash)) {
                fliesData = filesUtils.createFiles(file, contentType, currentFolderID, order);
            } else {
                //判断上级文件夹是否重命名了 重命名就更改文件路径
                if (type == 2) {
                    fliesData.setFileName(file.getName());
                    fliesData.setFilePath(file.getPath()); //判断修改文件信息
                    filesType = 2;
                }
            }
        } else {
            //不在 则为新的文件
            fliesData = filesUtils.createFiles(file, contentType, currentFolderID, order);
        }
        return new FileTaskResult(filesType, fliesData, parentPath);
    }
}

@Data
class FileTaskResult {
    short type;
    Files files;
    String parentPath;

    FileTaskResult(short type, Files files, String parentPath) {
        this.type = type;
        this.files = files;
        this.parentPath = parentPath;
    }
}