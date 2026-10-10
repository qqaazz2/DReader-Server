package io.github.qqaazz2.DReaderServer.task.utils;

import io.github.qqaazz2.DReaderServer.service.files.FilesService;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.common.TaskInterruptedException;
import io.github.qqaazz2.DReaderServer.entity.MetaData;
import io.github.qqaazz2.DReaderServer.entity.files.Files;
import io.github.qqaazz2.DReaderServer.enums.FilesCheckType;
import io.github.qqaazz2.DReaderServer.util.FileKeyAdapter;
import io.github.qqaazz2.DReaderServer.util.FilesUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileWalker {
    @Resource
    FilesUtils filesUtils;
    @Resource
    FilesService filesService;
    List<String> skipFolder = new ArrayList<>(List.of("#recycle", "@eaDir", "@Recycle", "metaData.json"));

    public ScanContext walk(File[] files, Long currentFolderID, ScanContext scanContext) {
        List<CheckFileTask> list = new ArrayList<>();
        List<FileTaskResult> fileTaskResultList = new ArrayList<>();
        deepFolder(files, currentFolderID, scanContext, list, fileTaskResultList);
        fileTaskResultList.addAll(fileWork(scanContext, list));
        preprocess(fileTaskResultList,scanContext);
        return scanContext;
    }

    private List<FileTaskResult> deepFolder(File[] files, Long currentFolderID, ScanContext scanContext, List<CheckFileTask> list, List<FileTaskResult> fileTaskResultList) {
        checkInterrupted();
        try {
            int index = 0;
            for (File file : files) {
                checkInterrupted();
                if (!skipFolder.isEmpty() && skipFolder.contains(file.getName())) continue;
                if (file.isDirectory()) {
                    onFolderFound(file, currentFolderID, scanContext, list, fileTaskResultList);
                } else if (file.isFile() && !filesUtils.isMetaFile(file)) {
                    list.add(new CheckFileTask(filesUtils, file, currentFolderID, index, scanContext));
                    index++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("扫描文件夹异常");
        }
        checkInterrupted();
        return fileTaskResultList;
    }

    private List<FileTaskResult> onFolderFound(File file, Long parentID, ScanContext scanContext, List<CheckFileTask> list, List<FileTaskResult> fileTaskResultList) throws IOException {
        checkInterrupted();
        //判断文件夹中没有metadata 如果没有则为新创建的文件夹
        //(所有没有的metadata文件的文件夹都会当作是新的文件夹，包括人为删除的)
        Files files;
        FilesCheckType type = FilesCheckType.NORMAL;
        File[] children = file.listFiles();
        int childCount = children == null ? 0 : children.length;

        //这里是没有matedata文件的文件夹
        if (!filesUtils.checkMetaFile(file.getPath())) {
            checkInterrupted();
            type = FilesCheckType.CREATE;
            files = filesUtils.createFolder(file, parentID, childCount);
            filesUtils.createMetaFile(file.getPath(),files.getId());
        } else {
            //获取文件夹中的metadata
            MetaData metaData = filesUtils.checkFolderId(file.getPath());
            String metaDataId = metaData.getId();
            files = scanContext.getFolderByIdMap().get(Long.parseLong(metaDataId));
            checkInterrupted();
            //如果metadata文件中的id和数据库中的值一样并且inode也一样就说明是同一条数据，否则将视为新的文件夹需要创建新的文件
            if (files != null && FileKeyAdapter.getFileKey(file).equals(files.getInode())) {
                //如果文件夹名称与数据库中名称不一样则为重命名
                //如果文件夹路径与数据库中路径不一样则为移动了
                //如果数据库中的父级ID和递归中传递的不一样可能是父级matedata文件变更或者其他特殊情况
                if ((!file.getName().equals(files.getFileName())) || !parentID.equals(files.getParentId()) || !file.getPath().equals(files.getFilePath())) {
                    files.setFileName(file.getName());
                    files.setFilePath(file.getPath());
                    files.setParentId(parentID);
                    type = FilesCheckType.RENAME;
                }
            } else {
                //这里是判断metadata中的id在数据库中不存在，不存在则当该文件夹为新建文件夹
                checkInterrupted();
                type = FilesCheckType.CREATE;
                files = filesUtils.createFolder(file, parentID, childCount);
                filesUtils.createMetaFile(file.getPath(),files.getId());
            }
        }


        checkInterrupted();
        if (childCount > 0) deepFolder(file.listFiles(), files.getId(), scanContext, list, fileTaskResultList);
        fileTaskResultList.add(new FileTaskResult(type, files));
        return fileTaskResultList;
    }


    protected List<FileTaskResult> fileWork(ScanContext scanContext, List<CheckFileTask> list) {
        List<Future<FileTaskResult>> futures = new ArrayList<>();
        List<FileTaskResult> fileTaskResultList = new ArrayList<>();
        ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(20000));;
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
                fileTaskResultList.add(future.get());
            }
        } catch (InterruptedException e) {
            log.warn("等待子任务完成时被中断，开始清理...");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            checkInterrupted();
        } catch (TaskInterruptedException e) {
            log.warn("任务在非阻塞阶段被中断...");
            executor.shutdownNow();
            checkInterrupted();
        } catch (Exception e) {
            e.printStackTrace();
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new BizException("4000", "文件扫描失败");
        }
        return fileTaskResultList;
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

    protected void checkInterrupted() {
        boolean interrupted = Thread.currentThread().isInterrupted();
        if (interrupted) {
            throw new TaskInterruptedException();
        }
    }

    @Transactional
    public void preprocess(List<FileTaskResult> fileTaskResultList, ScanContext scanContext) {
        List<Long> ids = new ArrayList<>();
        for (FileTaskResult fileTaskResult : fileTaskResultList) {
            checkInterrupted();
            Files files = fileTaskResult.getFiles();
            switch (fileTaskResult.getType()) {
                case CREATE -> scanContext.getCreateFiles().add(files);
                case RENAME -> {
                    scanContext.getRenameFiles().add(files);
                    ids.add(files.getId());
                    break;
                }
                case NORMAL -> ids.add(files.getId());
            }
        }
        scanContext.setRemoveFiles(scanContext.getFilesList().stream().filter(item -> !ids.contains(item.getId())).collect(Collectors.toList()));
    }
}
