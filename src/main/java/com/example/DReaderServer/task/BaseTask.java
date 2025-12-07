package com.example.DReaderServer.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.DReaderServer.entity.Author;
import com.example.DReaderServer.entity.Tags;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.files.FilesDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseTask {
    public AtomicBoolean running = new AtomicBoolean(false);
    public AtomicLong activeGeneration = new AtomicLong(0);

    @Async
    public void startOrRestart() {
        long newGen = activeGeneration.incrementAndGet();
        log.info("新的刮削任务已启动");
        while (!running.compareAndSet(false, true)) {
            activeGeneration.set(newGen);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        activeGeneration.set(newGen);
        log.info("新的刮削任务开始执行");
        try {
            start(newGen);
        } finally {
            running.set(false);
        }
    }

    public void stop() {
        running.set(false);
    }

    public AtomicBoolean getRunning() {
        return running;
    }

    public abstract void start(long currentGeneration);
}
