package com.example.DReaderServer.dto.bgm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BgmPersonsDTO {
    protected String name;
    protected String relation;
    protected Integer id;
    protected String summary;
    @JsonProperty("birth_year")
    protected Integer birthYear;
    @JsonProperty("birth_day")
    protected Integer birthDay;
    @JsonProperty("birth_mon")
    protected Integer birthMon;
    protected Map<String,String> images;
}
