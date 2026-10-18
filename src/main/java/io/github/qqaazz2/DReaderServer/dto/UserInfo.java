package io.github.qqaazz2.DReaderServer.dto;

import lombok.Data;

@Data
public class UserInfo {
    private String name;
    private String email;
    private int mystery;
    private String cover;
    private String fileAdapter;
    private String oldPassWord;
    private String newPassWord;
    private String archiveId;
    private String scraper;
    private Short proxyEnabled;
    private String proxyHost;
    private Short proxyPort;
}
