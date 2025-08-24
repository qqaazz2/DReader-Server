package com.example.DReaderServer.dto.book;

import com.example.DReaderServer.dto.QueryCondition;

public class BookListQueryCondition extends QueryCondition {

    private Integer id;
    private Integer status;
    private Integer folder;

    public Integer getFolder() {
        return folder;
    }

    public void setFolder(Integer folder) {
        this.folder = folder;
    }

    public BookListQueryCondition(Integer page) {
        super(page);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

}
