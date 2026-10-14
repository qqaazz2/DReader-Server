package io.github.qqaazz2.DReaderServer.dto.files;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FilesScraperQueryCondition {
    @NotNull(message = "书籍信息ID不可为空")
    private Integer id;
    private Integer selectId;
}
