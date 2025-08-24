package com.example.DReaderServer.service.impl.book;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.dto.PageVO;
import com.example.DReaderServer.dto.book.BookCoverDTO;
import com.example.DReaderServer.dto.book.BookListDTO;
import com.example.DReaderServer.dto.book.BookListQueryCondition;
import com.example.DReaderServer.dto.book.SeriesListQueryCondition;
import com.example.DReaderServer.entity.book.Book;
import com.example.DReaderServer.entity.book.BookFileCover;
import com.example.DReaderServer.mapper.book.BookMapper;
import com.example.DReaderServer.mapper.book.SeriesMapper;
import com.example.DReaderServer.service.UploadService;
import com.example.DReaderServer.service.book.BookService;
import com.example.DReaderServer.service.book.SeriesService;
import com.example.DReaderServer.util.FileTypeUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
@Transactional
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
    @Resource
    BookMapper bookMapper;

    @Resource
    SeriesService seriesService;

    @Resource
    UploadService uploadService;

    @Resource
    SeriesMapper seriesMapper;


    @Value("${file.upload}")
    String filePath;

    @Override
    public List<Book> createData(List<Book> list) {
        System.out.println(list);
        Boolean isTrue = this.saveBatch(list);
        if (!isTrue) throw new BizException("4000", "新增书籍信息失败");
        return list;
    }

    @Override
    public PageVO<BookListDTO> getList(BookListQueryCondition queryCondition) {
        List<BookListDTO> list = bookMapper.getList(queryCondition);
        list.stream().forEach(item -> item.setMinioCover(uploadService.getObject(item.getCover())));
        System.out.println(list);
        Integer count = bookMapper.count(queryCondition);
        return new PageVO(queryCondition.getLimit(), queryCondition.getPage(), count, list);
    }

    @Override
    public Book updateData(Book book) {
        Boolean isTrue = this.updateById(book);
        if (!isTrue) throw new BizException("4000", "编辑书籍信息失败");
        return book;
    }

    @Override
    public Map<String, Object> updateProgress(BookListDTO bookListDTO) {
        int status = bookListDTO.getProgress() >= 1 ? 2 : 3;
        LambdaUpdateWrapper<Book> updateWrapper = new LambdaUpdateWrapper();
        updateWrapper.eq(Book::getId, bookListDTO.getId())
                .set(Book::getStatus, status)
                .set(Book::getProgress, bookListDTO.getProgress())
                .set(Book::getReadTagNum, bookListDTO.getReadTagNum())
                .set(Book::getReadTime, new Date());
        Boolean isTrue = this.update(updateWrapper);
        if (!isTrue) throw new BizException("4000", "阅读进度修改失败");
        Map<String, Object> map = new HashMap<>();
        bookListDTO.setStatus(status);
        map.put("book", bookListDTO);
        map.put("status", 3);


        if (status == 2) {
            List<BookListDTO> list = bookMapper.getListByParentId(bookListDTO.getParentId());
            Long count = list.stream().filter(value -> value.getStatus() != 2).count();
            if (count == 0) {
                seriesService.updateStatus(bookListDTO.getParentId(), 2);
                map.put("status", 2);
            }
        } else {
            seriesService.updateStatus(bookListDTO.getParentId(), 3);
        }
        return map;
    }

    @Override
    public List<BookCoverDTO> getCoverList(Integer id) {
        return bookMapper.getCoverList(id);
    }

    @Override
    public BookListDTO getRecent() {
        List<BookListDTO> list = bookMapper.getRecent();
        if (bookMapper.getRecent().isEmpty()) {
            return null;
        }
        BookListDTO bookListDTO = list.get(0);
        bookListDTO.setMinioCover(uploadService.getObject(bookListDTO.getCover()));
        return bookListDTO;
    }

    @Override
    public Map<String, Integer> getOverview() {
        HashMap<String, Integer> map = new HashMap<>();
        Integer seriesCount = seriesMapper.count(new SeriesListQueryCondition(0));
        BookListQueryCondition queryCondition = new BookListQueryCondition(0);
        Integer bookCount = bookMapper.count(queryCondition);
        queryCondition.setStatus(2);
        Integer overCount = bookMapper.count(queryCondition);
        queryCondition.setStatus(1);
        Integer unreadCount = bookMapper.count(queryCondition);

        map.put("seriesCount", seriesCount);
        map.put("bookCount", bookCount);
        map.put("overCount", overCount);
        map.put("unreadCount", unreadCount);
        map.put("readingCount", bookCount - overCount - unreadCount);

        return map;
    }

    @Override
    @Transactional
    public Map<String, String> changeCover(Integer id, MultipartFile multipartFile) {
        FileTypeUtils.validateFile(multipartFile, new String[]{"jpg"}, 10240);
        Map<String, String> map = new HashMap<>();
        BookFileCover bookCover = bookMapper.getBookCover(id);
        if (bookCover == null) throw new BizException("4000", "找不到对应的书籍文件");
        String path = "";
        if (bookCover.getCover() == null || bookCover.getCover().isEmpty() || bookCover.getCover().isBlank()) {
            path = filePath + "books/" + bookCover.getParentFileName() + "/" + bookCover.getHash() + ".jpg";
            LambdaUpdateWrapper<Book> lambdaUpdateWrapper = new LambdaUpdateWrapper();
            lambdaUpdateWrapper.set(Book::getCover, path).eq(Book::getId, id);
            boolean isTrue = this.update(lambdaUpdateWrapper);
            if (!isTrue) throw new BizException("4000", "修改" + bookCover.getName() + "信息失败");
        } else path = bookCover.getCover();
        try {
            String minioUrl = uploadService.upload(multipartFile.getBytes(), path, "image/jpeg");
            map.put("minioUrl", uploadService.getObject(minioUrl));
            map.put("cover", path);
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            throw new BizException("4000", "修改" + bookCover.getName() + "封面图片失败");
        }
    }
}
