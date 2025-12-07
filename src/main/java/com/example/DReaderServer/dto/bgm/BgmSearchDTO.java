package com.example.DReaderServer.dto.bgm;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BgmSearchDTO implements Serializable {
    private String keyword;
    private String sort;
    private BgmSearchFilterDTO filter;
}


