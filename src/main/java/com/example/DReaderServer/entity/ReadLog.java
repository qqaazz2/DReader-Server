package com.example.DReaderServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadLog {

    @TableId(type = IdType.AUTO)
    protected Integer id;
    protected LocalDateTime time;
    protected Integer filesId;
    protected Integer seconds;

    @TableLogic
    protected Short deleted;
}
