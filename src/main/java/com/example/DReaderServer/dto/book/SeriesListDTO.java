package com.example.DReaderServer.dto.book;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Data
public class SeriesListDTO {
    private Integer id;
    private String name;
    private String cover;
    private String minioCover;
    private String author;
    private Integer overStatus;
    private Integer status;
    private Integer love;
    private String profile;
    private Date lastReadTime;
    private Integer num;
    private Integer filesId;
}
