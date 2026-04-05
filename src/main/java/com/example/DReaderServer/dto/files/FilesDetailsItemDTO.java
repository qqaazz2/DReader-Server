package com.example.DReaderServer.dto.files;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class FilesDetailsItemDTO {
    private Integer id;
    private String name;
    private String cover;
    private Integer overStatus;
    private Integer status;
    private Integer love;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long filesId;
    private Integer isFolder;
    private String lastReadTime;
    private String profile;
    private Integer bgmId;
    private String date;
    private String originalName;
    private String filePath;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentId;

    private List<FilesDetailsAuthor> filesAuthors = new ArrayList<>();
    private List<FilesDetailsTag> filesTags = new ArrayList<>();

    @Data
    public static class FilesDetailsAuthor {
        private String name;
        private Integer authorId;
        private Integer id;
    }

    @Data
    public static class FilesDetailsTag {
        private String name;
        private Integer tagId;
        private Integer id;
    }
}
