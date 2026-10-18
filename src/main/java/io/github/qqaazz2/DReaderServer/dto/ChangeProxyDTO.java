package io.github.qqaazz2.DReaderServer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeProxyDTO {
    @NotNull
    @Min(1)
    @Max(2)
    private Short proxyEnable;
    private String proxyHost;
    @Min(1)
    private Short proxyPort;
}