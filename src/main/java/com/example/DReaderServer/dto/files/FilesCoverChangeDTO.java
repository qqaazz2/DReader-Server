package com.example.DReaderServer.dto.files;

import lombok.Data;

@Data
public class FilesCoverChangeDTO {
    private Integer id;
    private String cover;
    private Integer isFolder;
    private Integer filesId;
    private Integer parentId;
    private String filePath;
    private Integer sort;
    private String name;
}
