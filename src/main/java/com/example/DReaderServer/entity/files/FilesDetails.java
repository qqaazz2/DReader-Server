package com.example.DReaderServer.entity.files;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.ibatis.annotations.Update;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class FilesDetails implements Serializable {
    @TableId(type = IdType.AUTO)
    @NotNull(groups = {Update.class},message = "ID不能为空")
    Integer id;
    @NotNull(groups = {Update.class},message = "名称不能为空")
    String name;
    Integer filesId;
    Integer bgmId;
    Integer overStatus;
    Integer status;
    Integer love;
    String profile;
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    Date lastReadTime;
    String date;
    String cover;
    String originalName;
    @TableLogic
    Integer deleted;

    @TableField(exist = false)
    private List<String> tags;
    @TableField(exist = false)
    private List<String> authors;

    @TableField(exist = false)
    String hash;
    @TableField(exist = false)
    Integer parentId;
    @TableField(exist = false)
    Integer isFolder;
}
