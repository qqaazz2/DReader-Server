package com.example.DReaderServer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.dto.UserInfo;
import com.example.DReaderServer.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService extends IService<User> {
    Map<String,Object> login(User user);

    Map<String,String> getCode();

    void verifyCode(String key,String code);

    UserInfo getUserInfo();

    UserInfo setUserInfo(UserInfo userInfo);

    void changeMystery(Integer mystery,String mysteryPassword);

    void updatePassWord(String oldPassWord,String newPassword);

    void updateMysteryPassWord(String oldPassWord,String newPassword);

    String updateImage(MultipartFile multipartFile);
}
