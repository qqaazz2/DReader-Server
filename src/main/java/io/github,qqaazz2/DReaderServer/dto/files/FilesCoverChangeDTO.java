package io.github.qqaazz2.DReaderServer.dto.files;

import lombok.Data;

@Data
public class FilesCoverChangeDTO {
    private Integer id;
    private String cover;
    private Integer isFolder;
    private Long filesId;
    private Long parentId;
    private String filePath;
    private Integer sort;
    private String name;
}
