package com.example.DReaderServer.dto.setting;

import lombok.Data;

import java.util.List;

@Data
public class TimeCountDTO {
    public TimeCountDTO(){}

    public TimeCountDTO(Integer time,Integer count){
        this.count = count;
        this.time = time;
    }

    private Integer time;
    private Integer count;
    private List<TimeCountDTO> children;
}