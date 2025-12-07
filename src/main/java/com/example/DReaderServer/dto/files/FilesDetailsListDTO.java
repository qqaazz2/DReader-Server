package com.example.DReaderServer.dto.files;

import lombok.Data;

import java.util.Date;

@Data
public class FilesDetailsListDTO {
    private Integer id;
    private String name;
    private String cover;
    private Integer overStatus;
    private Integer status;
    private Integer love;
    private Integer isFolder;
    private Integer filesId;
    private Integer parentId;
    private String filePath;
}
