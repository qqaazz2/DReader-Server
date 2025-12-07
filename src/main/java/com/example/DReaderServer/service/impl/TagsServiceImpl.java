package com.example.DReaderServer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.entity.Tags;
import com.example.DReaderServer.entity.files.FilesTags;
import com.example.DReaderServer.mapper.TagsMapper;
import com.example.DReaderServer.mapper.files.FilesTagsMapper;
import com.example.DReaderServer.service.TagsService;
import com.example.DReaderServer.service.files.FilesTagsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags> implements TagsService {

    @Override
    public Tags createTags(String name) {
        Tags tags = new Tags();
        tags.setName(name);
        if(!this.save(tags))throw new BizException("4000","创建失败标签");
        return tags;
    }

    @Override
    public List<Tags> getList() {
        return this.list();
    }

    @Override
    public List<Tags> createTags(List<Tags> list) {
        if(!this.saveBatch(list))throw new BizException("4000","创建失败标签");
        return list;
    }
}