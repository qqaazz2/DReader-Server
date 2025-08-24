package com.example.DReaderServer.entity.book;

import lombok.Data;

@Data
public class BookFileCover {
    private Long id;             // book.id
    private String cover;        // book.cover
    private String hash;         // files.hash
    private String parentFileName; // parent.file_name
    private String name;
}
