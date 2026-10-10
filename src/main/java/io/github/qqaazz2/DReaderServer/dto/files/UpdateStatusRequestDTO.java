package io.github.qqaazz2.DReaderServer.dto.files;

import lombok.Data;

import java.util.List;

@Data
public class UpdateStatusRequestDTO {
    private List<Integer> ids;
    private Integer status;
    private String field;
}
