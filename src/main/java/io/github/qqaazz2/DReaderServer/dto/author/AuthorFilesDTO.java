package io.github.qqaazz2.DReaderServer.dto.author;

import lombok.Data;

@Data
public class AuthorFilesDTO {
    long filesId;
    int filesAuthorId;
    String name;
    String cover;
}
