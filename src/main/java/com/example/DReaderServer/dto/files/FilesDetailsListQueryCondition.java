package com.example.DReaderServer.dto.files;

import com.example.DReaderServer.dto.QueryCondition;
import com.example.DReaderServer.dto.book.group.SpecificCheck;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FilesDetailsListQueryCondition extends QueryCondition {
    @NotNull(groups = {SpecificCheck.class}, message = "ID不可为空")
    private Integer id;
    @NotNull(groups = {SpecificCheck.class}, message = "喜欢状态不可为空")
    private Integer love;
    private Integer status;
    private String name;
    private String sortField;
    private String sortOrder;
    private Long parentId;
    private Integer isFolder;
    private Integer authorId;

    private Integer overStatus;
    private boolean flattening = true;

    public FilesDetailsListQueryCondition(Integer page) {
        super(page);
    }
}

