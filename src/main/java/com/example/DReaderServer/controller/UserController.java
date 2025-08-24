package com.example.DReaderServer.controller;

import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.common.UploadFile;
import com.example.DReaderServer.dto.UserInfo;
import com.example.DReaderServer.entity.User;
import com.example.DReaderServer.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;

    @Resource
    UploadFile uploadFile;

    @Resource
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResultResponse login(@RequestBody User user) {
        userService.verifyCode(user.getKey(), user.getCode());
        Map<String, Object> map = userService.login(user);
        return ResultResponse.success(map);
    }

    @GetMapping("/code")
    public ResultResponse getCode() {
        return ResultResponse.success(userService.getCode());
    }

    @GetMapping("/info")
    public ResultResponse info() {
        return ResultResponse.success(userService.getUserInfo());
    }

    @PostMapping("/setInfo")
    public ResultResponse setUserInfo(@RequestBody UserInfo userInfo) {
        System.out.println(userInfo.getName());
        return ResultResponse.success(userService.setUserInfo(userInfo));
    }

    @GetMapping("/changeMystery")
    public ResultResponse changeMystery(Integer mystery, String mysteryPassword) {
        userService.changeMystery(mystery, mysteryPassword);
        return ResultResponse.success();
    }

    @PostMapping("/updatePassWord")
    public ResultResponse updatePassWord(@RequestBody UserInfo userInfo) {
        userService.updatePassWord(userInfo.getOldPassWord(), userInfo.getNewPassWord());
        return ResultResponse.success();
    }

    @PostMapping("/uploadCover")
    public ResultResponse uploadCover(@RequestParam MultipartFile file) {
        return ResultResponse.success(userService.updateImage(file));
    }

    @PreAuthorize("@customPermissionEvaluator.hasPermission(authentication,null,1)")
    @PostMapping("/updateMysteryPassWord")
    public ResultResponse updateMysteryPassWord(@RequestBody UserInfo userInfo) {
        userService.updateMysteryPassWord(userInfo.getOldPassWord(), userInfo.getNewPassWord());
        return ResultResponse.success();
    }
}
