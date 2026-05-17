package io.github.qqaazz2.DReaderServer.task.utils;

import io.github.qqaazz2.DReaderServer.entity.files.Files;
import io.github.qqaazz2.DReaderServer.enums.FilesCheckType;
import lombok.Data;

@Data
public class FileTaskResult {
    FilesCheckType type;
    Files files;

    FileTaskResult(FilesCheckType type, Files files) {
        this.type = type;
        this.files = files;
    }
}
