package com.example.DReaderServer.dto.author;

import com.example.DReaderServer.dto.QueryCondition;
import com.example.DReaderServer.dto.book.group.SpecificCheck;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthorListQueryCondition extends QueryCondition {
    String name;
}

