package io.github.qqaazz2.DReaderServer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.dto.PageVO;
import io.github.qqaazz2.DReaderServer.dto.author.AuthorListDTO;
import io.github.qqaazz2.DReaderServer.dto.author.AuthorListQueryCondition;
import io.github.qqaazz2.DReaderServer.entity.Author;
import io.github.qqaazz2.DReaderServer.mapper.AuthorMapper;
import io.github.qqaazz2.DReaderServer.service.AuthorService;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorServiceImpl extends ServiceImpl<AuthorMapper, Author> implements AuthorService {

    private static final String AUTHORS = "scrape:authors";

    @Resource
    FileAdapterFactory factory;

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public Author createAuthor(Author author) {
        try {
            if (author.getAvatarFile() != null && !author.getAvatarFile().isEmpty())
                author.setAvatar(factory.getFileAdapter().uploadSplicing(author.getAvatarFile().getBytes(), "/authorAvatar/" + System.currentTimeMillis() + ".jpg", "image/jpeg"));
            author.setAvatarFile(null);
            if (!this.save(author)) throw new BizException("4000", "创建作者数据失败");
            redisTemplate.opsForHash().put(AUTHORS, author.getName(), author.getId());
            return author;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("4000", "创建作者数据失败");
        }
    }

    @Override
    public PageVO<AuthorListDTO> getList(AuthorListQueryCondition condition) {
        LambdaQueryWrapper<Author> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(condition.getName() != null && !condition.getName().isBlank(), Author::getName, condition.getName());
        Long count = this.count(queryWrapper);
        queryWrapper.select(Author::getName, Author::getAvatar, Author::getId).orderByDesc(Author::getId);
        List<Author> list = this.page(new Page<>(condition.getPage(), condition.getLimit()), queryWrapper).getRecords();
        List<AuthorListDTO> authorListDTOList = list.stream().map(item -> {
            AuthorListDTO dto = new AuthorListDTO();
            BeanUtils.copyProperties(item, dto);
            return dto;
        }).collect(Collectors.toList());
        return new PageVO<>(condition.getLimit(), condition.getPage(), count.intValue(), authorListDTOList);
    }

    @Override
    public Author getAuthor(int id) {
        Author author = this.getById(id);
        if (author == null) throw new BizException("4000", "找不到对应的制作者信息");
        return author;
    }

    @Override
    public Author updateAuthor(Author author) {
        try {
            Author oldAuthor = this.getById(author.getId());
            if (oldAuthor == null) throw new BizException("4000", "找不到对应的作者");
            if (author.getAvatarFile() != null && !author.getAvatarFile().isEmpty())
                author.setAvatar(factory.getFileAdapter().uploadSplicing(author.getAvatarFile().getBytes(), "/authorAvatar/" + System.currentTimeMillis() + ".jpg", "image/jpeg"));
            author.setAvatarFile(null);
            if (!this.updateById(author)) throw new BizException("4000", "编辑作者数据失败");
            if (!oldAuthor.getName().equals(author.getName())) {
                redisTemplate.opsForHash().delete(AUTHORS, oldAuthor.getName());
                redisTemplate.opsForHash().put(AUTHORS, author.getName(), author.getId());
            }
            return author;
        } catch (Exception e) {
            throw new BizException("4000", "编辑作者数据失败");
        }
    }

    @Override
    public List<Author> getList() {
        QueryWrapper<Author> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name");
        return this.list(queryWrapper);
    }

    @Override
    public List<Author> createAuthors(List<Author> list) {
        if (!this.saveBatch(list)) throw new BizException("4000", "创建作者失败");
        list.forEach(author -> redisTemplate.opsForHash().put(AUTHORS, author.getName(), author.getId()));
        return list;
    }
}