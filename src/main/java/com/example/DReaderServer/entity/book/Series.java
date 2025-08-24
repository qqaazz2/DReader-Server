package com.example.DReaderServer.entity.book;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Data
public class Series {
    @TableId(type = IdType.AUTO)
    @NotNull(groups = {Update.class},message = "系列ID不能为空")
    Integer id;

    Integer filesId;

    String author;

    Integer overStatus;

    Integer status;

    Integer deleted;

    Integer love;

    String profile;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    Date lastReadTime;

    Integer num;

    String cover;
    String addTime;
    String editTime;
}
