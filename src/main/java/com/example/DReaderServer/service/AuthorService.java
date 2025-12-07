package com.example.DReaderServer.service;

import com.example.DReaderServer.dto.PageVO;
import com.example.DReaderServer.dto.author.AuthorListDTO;
import com.example.DReaderServer.dto.author.AuthorListQueryCondition;
import com.example.DReaderServer.entity.Author;
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
