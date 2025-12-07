package com.example.DReaderServer.dto.bgm;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BgmSearchFilterDTO implements Serializable {
    private List<String> tag;
    private List<Integer> type;
    private Boolean nsfw;
}