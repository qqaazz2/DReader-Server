package com.example.DReaderServer.task.utils;

import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.enums.FilesMatchType;
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
