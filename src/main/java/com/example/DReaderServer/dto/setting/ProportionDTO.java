package com.example.DReaderServer.dto.setting;

import lombok.Data;

@Data
public class ProportionDTO {
    public ProportionDTO(Integer type, Integer count){
        this.type = type;
        this.count = count;
    }

    private Integer type;
    private Integer count;
}
