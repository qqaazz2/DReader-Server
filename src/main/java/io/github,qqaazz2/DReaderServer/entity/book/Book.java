package io.github.qqaazz2.DReaderServer.entity.book;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.ibatis.annotations.Update;

@Data
public class Book {
    @NotNull(groups = {Update.class},message = "书籍ID不能为空")
    @TableId(type = IdType.AUTO)
    Integer id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Long filesId;

    Double progress;

    Integer deleted;

    Integer readTagNum;
}
