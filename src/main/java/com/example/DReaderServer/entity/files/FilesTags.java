package com.example.DReaderServer.entity.files;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

@Data
public class FilesTags {
    @TableId(type = IdType.AUTO)
    protected Integer id;
    protected Long filesId;
    protected Integer tagsId;
    @TableLogic
    protected Integer deleted;
}
