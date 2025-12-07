package com.example.DReaderServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.DReaderServer.entity.Author;
import com.example.DReaderServer.entity.files.Files;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthorMapper extends BaseMapper<Author> {
}
