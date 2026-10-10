package io.github.qqaazz2.DReaderServer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.entity.Tags;
import io.github.qqaazz2.DReaderServer.mapper.TagsMapper;
import io.github.qqaazz2.DReaderServer.service.TagsService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags> implements TagsService {

    private static final String TAGS = "scrape:tags";

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public Tags createTags(String name) {
        Tags tags = new Tags();
        tags.setName(name);
        if (!this.save(tags)) throw new BizException("4000", "创建失败标签");
        redisTemplate.opsForHash().put(TAGS, tags.getName(), tags.getId());
        return tags;
    }

    @Override
    public List<Tags> getList() {
        return this.list();
    }

    @Override
    public List<Tags> createTags(List<Tags> list) {
        if (!this.saveBatch(list)) throw new BizException("4000", "创建失败标签");
        list.forEach(tags -> redisTemplate.opsForHash().put(TAGS, tags.getName(), tags.getId()));
        return list;
    }
}