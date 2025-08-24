package com.example.DReaderServer.mapper.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.DReaderServer.dto.book.BookCoverDTO;
import com.example.DReaderServer.dto.book.BookListDTO;
import com.example.DReaderServer.dto.book.BookListQueryCondition;
import com.example.DReaderServer.entity.book.Book;
import com.example.DReaderServer.entity.book.BookFileCover;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
    List<BookListDTO> getList(BookListQueryCondition queryCondition);

    Integer count(BookListQueryCondition queryCondition);

    List<BookListDTO> getListByParentId(Integer id);

    List<BookCoverDTO> getCoverList(Integer id);

    List<BookListDTO> getRecent();

    BookFileCover getBookCover(Integer id);
}
