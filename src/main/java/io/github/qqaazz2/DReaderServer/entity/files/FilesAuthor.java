package io.github.qqaazz2.DReaderServer.entity.files;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

@Data
public class FilesAuthor {
    @TableId(type = IdType.AUTO)
    protected Integer id;
    protected Long filesId;
    protected Integer authorId;
    @TableLogic
    protected Integer deleted;
}
