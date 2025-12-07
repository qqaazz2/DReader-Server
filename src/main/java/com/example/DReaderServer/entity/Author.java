package com.example.DReaderServer.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.springframework.data.annotation.Transient;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Date;

@Data
public class Author {
    @TableId(type = IdType.AUTO)
    @NotNull(groups = {Update.class},message = "作者ID不能为空")
    protected Integer id;
    @NotBlank(groups = {Insert.class,Update.class},message = "作者姓名不能为空")
    protected String name;
    protected String profile;
    protected String avatar;
    protected LocalDate date;
    protected String vocational;
    protected Integer bgmId;

    @Transient
    @JsonIgnore
    @TableField(exist = false)
    protected MultipartFile avatarFile;

    @TableLogic
    protected Integer deleted;
}
