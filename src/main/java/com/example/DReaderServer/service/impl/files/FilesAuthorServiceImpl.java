package com.example.DReaderServer.service.impl.files;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.dto.files.FilesDetailsItemDTO;
import com.example.DReaderServer.entity.Author;
import com.example.DReaderServer.entity.files.FilesAuthor;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.entity.files.FilesTags;
import com.example.DReaderServer.mapper.files.FilesAuthorMapper;
import com.example.DReaderServer.mapper.files.FilesTagsMapper;
import com.example.DReaderServer.service.files.FilesAuthorService;
import com.example.DReaderServer.service.files.FilesTagsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FilesAuthorServiceImpl extends ServiceImpl<FilesAuthorMapper, FilesAuthor> implements FilesAuthorService {
    @Override
    public List<FilesAuthor> createList(List<FilesAuthor> list) {
        if (!this.saveBatch(list)) throw new BizException("4000", "创建关联数据失败");
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 记得加事务
    public List<FilesDetailsItemDTO.FilesDetailsAuthor> saveDataByFilesId(Long filesId, List<FilesDetailsItemDTO.FilesDetailsAuthor> list) {
        LambdaQueryWrapper<FilesAuthor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FilesAuthor::getFilesId, filesId);
        List<FilesAuthor> dbList = this.list(queryWrapper);
        Set<Integer> authorIds = list.stream().map(FilesDetailsItemDTO.FilesDetailsAuthor::getAuthorId).collect(Collectors.toSet());
        List<Integer> deleteIds = dbList.stream()
                .filter(item -> !authorIds.contains(item.getAuthorId()))
                .map(FilesAuthor::getId)
                .collect(Collectors.toList());
        if (!deleteIds.isEmpty())if (!this.removeByIds(deleteIds)) throw new BizException("4000", "删除绑定作者失败");
        Set<Integer> dbAuthorIds = dbList.stream().map(FilesAuthor::getAuthorId).collect(Collectors.toSet());
        List<FilesAuthor> addList = authorIds.stream()
                .filter(authorId -> !dbAuthorIds.contains(authorId))
                .map(authorId -> {
                    FilesAuthor filesAuthor = new FilesAuthor();
                    filesAuthor.setFilesId(filesId);
                    filesAuthor.setAuthorId(authorId);
                    return filesAuthor;
                }).collect(Collectors.toList());
        if (!addList.isEmpty())  this.createList(addList);
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
        return list;
    }
}