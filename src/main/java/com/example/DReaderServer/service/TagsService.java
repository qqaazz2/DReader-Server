package com.example.DReaderServer.service;

import com.example.DReaderServer.entity.Tags;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TagsService {
    Tags createTags(String name);

    List<Tags> getList();

    List<Tags> createTags(List<Tags> list);
}
