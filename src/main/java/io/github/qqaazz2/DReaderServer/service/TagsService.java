package io.github.qqaazz2.DReaderServer.service;

import io.github.qqaazz2.DReaderServer.entity.Tags;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TagsService {
    Tags createTags(String name);

    List<Tags> getList();

    List<Tags> createTags(List<Tags> list);
}
