package com.example.DReaderServer.task;

import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.common.TaskInterruptedException;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.MetaData;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.enums.FilesCheckType;
import com.example.DReaderServer.enums.FilesMatchType;
import com.example.DReaderServer.service.files.FilesDetailsService;
import com.example.DReaderServer.service.files.FilesService;
import com.example.DReaderServer.task.utils.FileTaskResult;
import com.example.DReaderServer.task.utils.FileWalker;
import com.example.DReaderServer.task.utils.ScanContext;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Transactional
public abstract class AsyncTask {
    @Resource
    AsyncTaskExecutor taskExecutor;
    @Resource
    FilesUtils filesUtils;
    @Resource
    FilesService filesService;
    @Resource
    ScrapeTask scrapeTask;
    @Resource
    FilesDetailsService filesDetailsService;
    @Resource
    FileWalker fileWalker;

    @Value("${file.upload}")
    String filePath;

    String basePath;
    public static ConcurrentHashMap<Class<?>, Thread> taskMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Class<?>, Integer> taskNumMap = new ConcurrentHashMap<>();

    public void startOrRestart(String path) {
        Class<?> taskClass = this.getClass();
        String taskName = Thread.currentThread().getName() + "-" + taskClass.getSimpleName();
        log.info("[{}] 新任务已启动", taskName);
        Thread oldTask = taskMap.get(taskClass);
        scrapeTask.stop();
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
        try {
            List<Files> filesList = filesService.getFilesList();
            ScanContext scanContext = new ScanContext(filePath + (((path.equals("") || path.isEmpty()) ? basePath : path)), filesList);
            filesUtils.checkMetaFile(scanContext.getResourcesFile().getPath());
            checkInterrupted();
            scanContext = fileWalker.walk(scanContext.getResourcesFile().listFiles(), -1L, scanContext);
            checkInterrupted();
            if (!scanContext.getCreateFiles().isEmpty()) create(scanContext);
            rename(scanContext.getRenameFiles());
            checkInterrupted();
            remove(scanContext.getRemoveFiles());
            log.info("扫描完成");
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        scrapeTask.startOrRestart();
                    }
                });
            } else {
                scrapeTask.startOrRestart();
            }
        } catch (TaskInterruptedException e) {
            log.warn("任务在非阻塞阶段被中断...");
            checkInterrupted();
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            throw new BizException("4000", "文件扫描失败");
        }
    }


    public void rename(List<Files> renameFiles) {
        checkInterrupted();
        if(renameFiles.isEmpty()) return;
        filesService.renameFiles(renameFiles);
    }

    public abstract void create(ScanContext scanContext);

    public void remove(List<Files> removeList) {
        checkInterrupted();
        if(removeList.isEmpty()) return;
        filesService.removerFiles(removeList); //删除数据库中的数据
    }

    @PreDestroy
    public void onDestroy() {
//        executor.shutdown();
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