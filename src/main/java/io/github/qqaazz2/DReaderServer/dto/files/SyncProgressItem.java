package io.github.qqaazz2.DReaderServer.dto.files;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SyncProgressItem {
    @NotNull(message = "hash值不能为空")
    String hash;
    Double progress;
    Integer readTagNum;
}
