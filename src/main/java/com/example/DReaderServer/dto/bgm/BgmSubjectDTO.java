package com.example.DReaderServer.dto.bgm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BgmSubjectDTO implements Serializable {
    protected String date;
    protected String platform;
    protected Boolean series;
    protected String name;
    @JsonProperty("name_cn")
    protected String nameCn;
    protected String summary;
    protected Integer id;
    protected String relation;
    protected List<SubjectTag> tags;
    protected List<SubjectInfobox> infobox;

    @Data
    public static class SubjectTag {
        protected String name;
    }

    @Data
    public static class SubjectInfobox {
        protected String key;
        protected Object value;
    }
}