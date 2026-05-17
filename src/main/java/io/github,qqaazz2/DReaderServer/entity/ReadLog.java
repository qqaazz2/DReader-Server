package io.github.qqaazz2.DReaderServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadLog {

    @TableId(type = IdType.AUTO)
    protected Integer id;
    protected LocalDateTime time;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    protected Long filesId;
    protected Integer seconds;

    @TableLogic
    protected Short deleted;
}
