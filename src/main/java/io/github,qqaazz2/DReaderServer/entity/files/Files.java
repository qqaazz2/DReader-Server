package io.github.qqaazz2.DReaderServer.entity.files;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.File;
import java.util.Date;
import java.util.List;

@Data
public class Files {

    @TableId(type = IdType.ASSIGN_ID)
    protected Long id;

    @TableField(fill = FieldFill.INSERT)
    protected Date add_time;
    protected Date edit_time;

    @TableLogic
    protected Integer deleted;
    protected Integer status;

    private String fileName;
    private Long fileSize;
    private String filePath;
    private String fileType;
    private String fileNote;
    private Integer isFolder = 2;
    private Long parentId;
    private String hash;
    private Integer sort;
    private String inode;
    @TableField(exist = false)
    private File file;
    @TableField(exist = false)
    private Object other;
    @TableField(exist = false)
    private List<Files> child;
    @TableField(exist = false)
    private Integer count;
    @TableField(exist = false)
    private String fakeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Files files = (Files) o;

        if (!(id == files.id)) return false;
        return filePath.equals(files.filePath);
    }

    public void addChild(Files child) {
        this.child.add(child);
    }
}
