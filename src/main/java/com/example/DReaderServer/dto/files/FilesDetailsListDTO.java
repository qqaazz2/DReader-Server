package com.example.DReaderServer.dto.files;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long filesId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentId;
    private String filePath;
}
