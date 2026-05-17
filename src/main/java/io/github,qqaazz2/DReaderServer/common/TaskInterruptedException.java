package io.github.qqaazz2.DReaderServer.common;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskInterruptedException extends RuntimeException{
    public TaskInterruptedException(){
        super();
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
