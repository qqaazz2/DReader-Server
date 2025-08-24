package com.example.DReaderServer.dto.book;

import lombok.Data;

import java.util.Date;

@Data
public class BookListDTO {
    private Integer id;
    private Integer filesId;
    private String filePath;
    private Integer isFolder;
    private String name;
    private String author;
    private String profile;
    private Double progress;
    private Integer status;
    private String publishing;
    private Integer parentId;
    private String cover;
    private String minioCover;
    private Integer readTagNum;
}
