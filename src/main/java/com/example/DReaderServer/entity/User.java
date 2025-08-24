package com.example.DReaderServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class User implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String email;
    private String password;
    private Integer theme;
    private Date expireTime;
    private Integer mystery;
    private String mysteryPassword;
    private String cover;


    //验证码
    @TableField(exist = false)
    private String code;
    @TableField(exist = false)
    private String key;
}
