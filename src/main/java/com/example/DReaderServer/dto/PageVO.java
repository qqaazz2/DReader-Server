package com.example.DReaderServer.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {
    private int limit;
    private int page;
    private int pages;
    private int count;
    private List<T> data;

    public PageVO(int pageSize, int pageNum, int total, List<T> data) {
        this.limit = pageSize;
        this.page = pageNum;
        this.count = total;
        this.data = data;
        this.pages = total / pageSize + (total % pageSize == 0 ? 0 : 1);
    }
}
