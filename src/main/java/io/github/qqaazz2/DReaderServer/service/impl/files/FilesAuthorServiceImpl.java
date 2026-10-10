package io.github.qqaazz2.DReaderServer.service.impl.files;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.qqaazz2.DReaderServer.service.files.FilesAuthorService;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.dto.files.FilesDetailsItemDTO;
import io.github.qqaazz2.DReaderServer.entity.files.FilesAuthor;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesAuthorMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FilesAuthorServiceImpl extends ServiceImpl<FilesAuthorMapper, FilesAuthor> implements FilesAuthorService {

    private static final String AUTHOR_FILES = "scrape:author_files";

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<FilesAuthor> createList(List<FilesAuthor> list) {
        if (!this.saveBatch(list)) throw new BizException("4000", "创建关联数据失败");
        list.forEach(item -> {
            List<Integer> authorIds = (List<Integer>) redisTemplate.opsForHash().get(AUTHOR_FILES, item.getFilesId());
            if (authorIds == null) authorIds = new ArrayList<>();
            if (!authorIds.contains(item.getAuthorId())) authorIds.add(item.getAuthorId());
            redisTemplate.opsForHash().put(AUTHOR_FILES, item.getFilesId(), authorIds);
        });
        return list;
    }

    @Override
    public void removeByFilesIds(List<Long> filesIds) {
        if (filesIds == null || filesIds.isEmpty()) return;
        LambdaQueryWrapper<FilesAuthor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(FilesAuthor::getFilesId, filesIds);
        boolean success = this.remove(lambdaQueryWrapper);
        if (!success) {
            log.error("系列或书籍作者数据不存在");
        }
        filesIds.forEach(filesId -> redisTemplate.opsForHash().delete(AUTHOR_FILES, filesId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FilesDetailsItemDTO.FilesDetailsAuthor> saveDataByFilesId(Long filesId, List<FilesDetailsItemDTO.FilesDetailsAuthor> list) {
        LambdaQueryWrapper<FilesAuthor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FilesAuthor::getFilesId, filesId);
        List<FilesAuthor> dbList = this.list(queryWrapper);
        Set<Integer> authorIds = list.stream().map(FilesDetailsItemDTO.FilesDetailsAuthor::getAuthorId).collect(Collectors.toSet());
        List<Integer> deleteIds = dbList.stream()
                .filter(item -> !authorIds.contains(item.getAuthorId()))
                .map(FilesAuthor::getId)
                .collect(Collectors.toList());
        if (!deleteIds.isEmpty()) if (!this.removeByIds(deleteIds)) throw new BizException("4000", "删除绑定作者失败");

        Set<Integer> dbAuthorIds = dbList.stream().map(FilesAuthor::getAuthorId).collect(Collectors.toSet());
        List<FilesAuthor> addList = authorIds.stream()
                .filter(authorId -> !dbAuthorIds.contains(authorId))
                .map(authorId -> {
                    FilesAuthor filesAuthor = new FilesAuthor();
                    filesAuthor.setFilesId(filesId);
                    filesAuthor.setAuthorId(authorId);
                    return filesAuthor;
                }).collect(Collectors.toList());

        if (!addList.isEmpty()) this.createList(addList);

        Map<Integer, Integer> fullMap = new HashMap<>();
        dbList.stream()
                .filter(item -> authorIds.contains(item.getAuthorId()))
                .forEach(item -> fullMap.put(item.getAuthorId(), item.getId()));
        addList.forEach(item -> fullMap.put(item.getAuthorId(), item.getId()));

        list.forEach(item -> {
            if (fullMap.containsKey(item.getAuthorId())) {
                item.setId(fullMap.get(item.getAuthorId()));
            }
        });
        redisTemplate.opsForHash().put(AUTHOR_FILES, filesId, new ArrayList<>(authorIds));
        return list;
    }
}