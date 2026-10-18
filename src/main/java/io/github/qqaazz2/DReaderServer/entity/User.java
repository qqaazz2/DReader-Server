package io.github.qqaazz2.DReaderServer.entity;

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
    private String fileAdapter;
    private String scraper;
    private String archiveId;
    private Short proxyEnabled;
    private String proxyHost;
    private Short proxyPort;


    //验证码
    @TableField(exist = false)
    private String code;
    @TableField(exist = false)
    private String key;
}
