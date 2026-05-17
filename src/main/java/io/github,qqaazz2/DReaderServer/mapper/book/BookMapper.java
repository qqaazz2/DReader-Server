package io.github.qqaazz2.DReaderServer.mapper.book;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qqaazz2.DReaderServer.dto.book.BookCoverDTO;
import io.github.qqaazz2.DReaderServer.dto.book.BookListDTO;
import io.github.qqaazz2.DReaderServer.dto.book.BookListQueryCondition;
import io.github.qqaazz2.DReaderServer.entity.book.Book;
import io.github.qqaazz2.DReaderServer.entity.book.BookFileCover;
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
