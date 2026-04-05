package com.example.DReaderServer.task.utils;

import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.enums.FilesCheckType;
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
