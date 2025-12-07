package com.example.DReaderServer.controller;

import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.dto.author.AuthorListQueryCondition;
import com.example.DReaderServer.entity.Author;
import com.example.DReaderServer.service.AuthorService;
import com.example.DReaderServer.service.TagsService;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/author")
public class AuthorController {
    @Resource
    AuthorService authorService;

    @GetMapping("/getAuthorList")
    public ResultResponse getAuthorList(AuthorListQueryCondition queryCondition) {
        return ResultResponse.success(authorService.getList(queryCondition));
    }

    @GetMapping("/getAuthorListAll")
    public ResultResponse getAuthorListAll() {
        return ResultResponse.success(authorService.getList());
    }

    @GetMapping("/getAuthorDetail")
    public ResultResponse getAuthorDetail(@RequestParam Integer id) {
        return ResultResponse.success(authorService.getAuthor(id));
    }

    @PostMapping("/updateAuthor")
    public ResultResponse updateAuthor(@Validated({Update.class}) Author author) {
        return ResultResponse.success(authorService.updateAuthor(author));
    }

    @PostMapping("/create")
    public ResultResponse create(@Validated({Insert.class}) Author author) {
        return ResultResponse.success(authorService.createAuthor(author));
    }

    @PostMapping("/update")
    public ResultResponse update(@Validated({Update.class}) Author author) {
        return ResultResponse.success(authorService.updateAuthor(author));
    }
}
