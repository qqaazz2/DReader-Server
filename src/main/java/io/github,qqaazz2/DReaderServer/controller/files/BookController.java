package io.github.qqaazz2.DReaderServer.controller.files;

import io.github.qqaazz2.DReaderServer.common.ResultResponse;
import io.github.qqaazz2.DReaderServer.entity.book.Book;
import io.github.qqaazz2.DReaderServer.service.book.BookService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/book")
public class BookController {

//
    @Resource
BookService bookService;
//
    @GetMapping("getData")
    public ResultResponse getData(@RequestParam Long filesId) {
        return ResultResponse.success(bookService.getData(filesId));
    }
//
//    @PostMapping("updateData")
//    public ResultResponse updateData(@RequestBody @Validated({Update.class}) Book book) {
//        bookService.updateData(book);
//        return ResultResponse.success();
//    }
//
    @PostMapping("updateProgress")
    public ResultResponse updateProgress(@RequestBody Book book) {
        return ResultResponse.success(bookService.updateProgress(book));
    }
//
//    @GetMapping("getCoverList")
//    public ResultResponse getCoverList(Integer id) {
//        Map<String, List<BookCoverDTO>> map = new HashMap<>();
//        map.put("list", bookService.getCoverList(id));
//        return ResultResponse.success(map);
//    }
//
//    @GetMapping("getRecent")
//    public ResultResponse getRecent() {
//        return ResultResponse.success(bookService.getRecent());
//    }
//
//    @GetMapping("getOverview")
//    public ResultResponse getOverview() {
//        return ResultResponse.success(bookService.getOverview());
//    }
//
//    @PostMapping("/changeCover")
//    public ResultResponse changeCover(@RequestParam MultipartFile file, @RequestParam Integer id) {
//        return ResultResponse.success(bookService.changeCover(id, file));
//    }
}
