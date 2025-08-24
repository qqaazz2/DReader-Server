package com.example.DReaderServer.dto;

import lombok.Data;

@Data
public class QueryCondition {
    private Integer page;
    private Integer limit;
    private Integer offset;

    public QueryCondition(){};

    public QueryCondition(Integer page) {
        this.page = page;
    }

    public Integer getOffset() {
        return ((page == null || page < 1 ? 1 : page) - 1) * (limit == null ? 10 : limit);
    }
}
