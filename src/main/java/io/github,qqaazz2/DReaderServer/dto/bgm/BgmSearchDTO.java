package io.github.qqaazz2.DReaderServer.dto.bgm;

import lombok.Data;

import java.io.Serializable;

@Data
public class BgmSearchDTO implements Serializable {
    private String keyword;
    private String sort;
    private BgmSearchFilterDTO filter;
}


