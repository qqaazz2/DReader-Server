package io.github.qqaazz2.DReaderServer.dto.book;

import io.github.qqaazz2.DReaderServer.dto.QueryCondition;
import io.github.qqaazz2.DReaderServer.dto.book.group.SpecificCheck;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeriesListQueryCondition extends QueryCondition {
    @NotNull(groups = {SpecificCheck.class}, message = "ID不可为空")
    private Integer id;
    @NotNull(groups = {SpecificCheck.class}, message = "喜欢状态不可为空")
    private Integer love;
    private Integer status;
    private String name;
    private String sortField;
    private String sortOrder;

    private Integer overStatus;
    private boolean flattening = true;

    public SeriesListQueryCondition(Integer page) {
        super(page);
    }
}

