package io.github.qqaazz2.DReaderServer.channel;

import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.task.AsyncTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class ScanningSseClient {
    private static SseEmitter sseEmitter;
    public SseEmitter createSse() {
        sseEmitter = new SseEmitter(0l);
        sseEmitter.onCompletion(() -> {
            log.info("结束Sse连接");
        });
        sseEmitter.onTimeout(() -> {
            log.info("Sse连接超时");
        });
        sseEmitter.onError(
                throwable -> {
                    try {
                        log.info("Sse连接异常,{}", throwable.toString());
                    } catch (Exception e) {
                        throw new BizException("4000", "Sse服务连接错误");
                    }
                }
        );
        return sseEmitter;
    }

    @Async
    public void sendMessage() {
        try {
            sseEmitter.send(SseEmitter.event()
                    .id("task-running")
                    .name("status")
                    .data("队列任务执行中，请稍候..."));
            Thread.sleep(5000);
            while (AsyncTask.taskMap.size() > 0) {
                sseEmitter.send(SseEmitter.event()
                        .id("task-running")
                        .name("status")
                        .data("队列任务执行中，请稍候..."));
                Thread.sleep(5000);
            }
            sseEmitter.send(SseEmitter.event()
                    .id("task-finished")
                    .name("status")
                    .data("所有任务均已完成，系统空闲"));
            Thread.sleep(5000);
            // 先清理上下文再关闭 SSE
            SecurityContextHolder.clearContext();
            sseEmitter.complete();

            log.info("SSE 已完成任务并关闭连接");
        } catch (Exception e) {
            throw new BizException("4000", "Sse服务连接错误");
        }
    }


    public void closeSse() {
        sseEmitter.complete();
    }
}
