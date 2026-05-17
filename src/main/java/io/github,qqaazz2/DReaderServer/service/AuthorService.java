package io.github.qqaazz2.DReaderServer.service;

import io.github.qqaazz2.DReaderServer.dto.PageVO;
import io.github.qqaazz2.DReaderServer.dto.author.AuthorListDTO;
import io.github.qqaazz2.DReaderServer.dto.author.AuthorListQueryCondition;
import io.github.qqaazz2.DReaderServer.entity.Author;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface AuthorService {
    Author createAuthor(Author author);

    PageVO<AuthorListDTO> getList(AuthorListQueryCondition authorListQueryCondition);

    Author getAuthor(int id);

    Author updateAuthor(Author author);

    List<Author> getList();
}
