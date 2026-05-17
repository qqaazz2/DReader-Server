package io.github.qqaazz2.DReaderServer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.entity.Tags;
import io.github.qqaazz2.DReaderServer.mapper.TagsMapper;
import io.github.qqaazz2.DReaderServer.service.TagsService;
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