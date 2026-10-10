package io.github.qqaazz2.DReaderServer.task.utils;

import io.github.qqaazz2.DReaderServer.entity.files.Files;
import io.github.qqaazz2.DReaderServer.enums.FilesMatchType;
import lombok.Data;

@Data
public class FilesMatchResult {
    private FilesMatchType type;
    private Files matchedFile;
    private Integer hashNum;

    public FilesMatchResult(FilesMatchType exact, Files files, Integer hashNum) {
        this.type = exact;
        this.matchedFile = files;
        this.hashNum = hashNum;
    }
}
