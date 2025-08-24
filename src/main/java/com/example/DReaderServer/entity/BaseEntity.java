package com.example.DReaderServer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
public class BaseEntity {
    @TableId(type = IdType.AUTO)
    protected Integer id;

    @TableField(fill = FieldFill.INSERT)
    protected Date add_time;
    protected Date edit_time;

    @TableLogic
    protected Integer deleted;
    protected Integer status;
}
