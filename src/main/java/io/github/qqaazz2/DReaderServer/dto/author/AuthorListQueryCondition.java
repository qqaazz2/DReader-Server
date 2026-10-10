package io.github.qqaazz2.DReaderServer.dto.author;

import io.github.qqaazz2.DReaderServer.dto.QueryCondition;
import lombok.Data;

@Data
public class AuthorListQueryCondition extends QueryCondition {
    String name;
}

