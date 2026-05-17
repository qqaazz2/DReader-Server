package io.github.qqaazz2.DReaderServer.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
