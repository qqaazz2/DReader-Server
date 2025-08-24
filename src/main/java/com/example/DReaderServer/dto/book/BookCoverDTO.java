package com.example.DReaderServer.dto.book;

import lombok.Data;

@Data
public class BookCoverDTO {
    private Integer id;
    private String name;
    private String cover;
    private String coverPath;
}
