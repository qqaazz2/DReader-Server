package io.github.qqaazz2.DReaderServer.task;

import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.common.TaskInterruptedException;
import io.github.qqaazz2.DReaderServer.entity.files.Files;
import io.github.qqaazz2.DReaderServer.service.files.FilesService;
import io.github.qqaazz2.DReaderServer.task.utils.FileWalker;
import io.github.qqaazz2.DReaderServer.task.utils.ScanContext;
import io.github.qqaazz2.DReaderServer.util.FilesUtils;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Transactional
public abstract class AsyncTask {
    @Resource
    FilesUtils filesUtils;
    @Resource
    FilesService filesService;
    @Resource
    io.github.qqaazz2.DReaderServer.task.ScrapeTask scrapeTask;
    @Resource
    FileWalker fileWalker;

    @Value("${file.upload}")
    String filePath;

    String basePath;
    public static ConcurrentHashMap<Class<?>, Thread> taskMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Class<?>, Integer> taskNumMap = new ConcurrentHashMap<>();

    public void startOrRestart(String path) {
        Class<?> taskClass = this.getClass();
        Thread currentThread = Thread.currentThread();
        String taskName = Thread.currentThread().getName() + "-" + taskClass.getSimpleName();
        log.info("[{}] 新任务已启动", taskName);
        Thread oldTask = taskMap.put(taskClass, currentThread);
        scrapeTask.stop();
        if (oldTask != null && oldTask.isAlive()) {
            log.info("发现旧任务[{}]正在运行，准备中止...", oldTask.getName() + "-" + taskClass.getSimpleName());
            oldTask.interrupt();
            try {
                oldTask.join();
            } catch (InterruptedException e) {
                currentThread.interrupt();
                throw new TaskInterruptedException();
            }
            log.info("[{}]任务已完全终止", oldTask.getName() + "-" + taskClass.getSimpleName());
        }

        try {
            start(path);
        } catch (TaskInterruptedException e) {
            log.info("[{}]任务被中断", taskName);
            throw e;
        } catch (Exception e) {
            log.error("[{}] 任务执行异常", taskName, e);
            throw e;
        } finally {
            taskMap.remove(taskClass, currentThread);
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
        if (renameFiles.isEmpty()) return;
        filesService.renameFiles(renameFiles);
    }

    public abstract void create(ScanContext scanContext);

    public void remove(List<Files> removeList) {
        checkInterrupted();
        if (removeList.isEmpty()) return;
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