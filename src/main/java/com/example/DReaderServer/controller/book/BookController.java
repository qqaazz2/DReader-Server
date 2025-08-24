package com.example.DReaderServer.controller.book;

import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.dto.PageVO;
import com.example.DReaderServer.dto.book.*;
import com.example.DReaderServer.entity.book.Book;
import com.example.DReaderServer.service.book.BookService;
import com.example.DReaderServer.task.BookTask;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Update;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/book")
public class BookController {
    @Resource
    BookTask bookTask;

    @Resource
    BookService bookService;

    @GetMapping("scanning")
    public ResultResponse scanning(@RequestParam(defaultValue = "", required = false) String path) {
        bookTask.startOrRestart(path);
        return ResultResponse.success();
    }

    @GetMapping("getList")
    public ResultResponse getList(BookListQueryCondition queryCondition) {
        return ResultResponse.success(bookService.getList(queryCondition));
    }

    @PostMapping("updateData")
    public ResultResponse updateData(@RequestBody @Validated({Update.class}) Book book) {
        bookService.updateData(book);
        return ResultResponse.success();
    }

    @PostMapping("updateProgress")
    public ResultResponse updateProgress(@RequestBody @Validated({Update.class}) BookListDTO book) {
        Map<String, Object> map = bookService.updateProgress(book);
        return ResultResponse.success(map, "更新阅读进度成功");
    }

    @GetMapping("getCoverList")
    public ResultResponse getCoverList(Integer id) {
        Map<String, List<BookCoverDTO>> map = new HashMap<>();
        map.put("list", bookService.getCoverList(id));
        return ResultResponse.success(map);
    }

    @GetMapping("getRecent")
    public ResultResponse getRecent() {
        return ResultResponse.success(bookService.getRecent());
    }

    @GetMapping("getOverview")
    public ResultResponse getOverview() {
        return ResultResponse.success(bookService.getOverview());
    }

    @PostMapping("/changeCover")
    public ResultResponse changeCover(@RequestParam MultipartFile file, @RequestParam Integer id) {
        return ResultResponse.success(bookService.changeCover(id, file));
    }
}
