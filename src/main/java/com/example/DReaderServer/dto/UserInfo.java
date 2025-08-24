package com.example.DReaderServer.dto;

import lombok.Data;

@Data
public class UserInfo {
    private String name;
    private String email;
    private int mystery;
    private String cover;
    private String minioCover;

    private String oldPassWord;
    private String newPassWord;
}
