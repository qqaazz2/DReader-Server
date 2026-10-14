package io.github.qqaazz2.DReaderServer.dto.bgm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
public class SearchSubjectsResponse {
    private List<BgmSubjectDTO> data;
    private Integer total;
    private Integer limit;
    private Integer offset;
}