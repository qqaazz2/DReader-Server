package io.github.qqaazz2.DReaderServer.service.book;

import io.github.qqaazz2.DReaderServer.entity.book.Book;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface BookService {
    List<Book> createData(List<Book> list);

    Book getData(Long filesId);

    Book createData(Long filesId);
//
//    PageVO<BookListDTO> getList(BookListQueryCondition queryCondition);
//
//    Book updateData(Book book);
//
    Map<String,Object> updateProgress(Book book);
//
//    List<BookCoverDTO> getCoverList(Integer id);
//
//    BookListDTO getRecent();
//
//    Map<String,Integer> getOverview();
//
//    Map<String,String> changeCover(Integer id,MultipartFile file);
}
