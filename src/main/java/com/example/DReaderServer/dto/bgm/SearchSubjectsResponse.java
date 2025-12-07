package com.example.DReaderServer.dto.bgm;

import lombok.Data;

import java.util.List;

@Data
public class SearchSubjectsResponse {
    private List<BgmSubjectDTO> data;
    private Integer total;
    private Integer limit;
    private Integer offset;
}