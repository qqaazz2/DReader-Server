package com.example.DReaderServer.service.impl.files;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.dto.files.FilesDetailsItemDTO;
import com.example.DReaderServer.dto.setting.ProportionDTO;
import com.example.DReaderServer.dto.setting.TimeCountDTO;
import com.example.DReaderServer.entity.Tags;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.files.FilesTags;
import com.example.DReaderServer.mapper.files.FilesMapper;
import com.example.DReaderServer.mapper.files.FilesTagsMapper;
import com.example.DReaderServer.service.TagsService;
import com.example.DReaderServer.service.files.FilesService;
import com.example.DReaderServer.service.files.FilesTagsService;
import com.example.DReaderServer.util.ValidationUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FilesTagsServiceImpl extends ServiceImpl<FilesTagsMapper, FilesTags> implements FilesTagsService {

    @Resource
    TagsService tagsService;

    @Override
    public List<FilesTags> createList(List<FilesTags> list) {
        if (!this.saveBatch(list)) throw new BizException("4000", "创建关联标签失败");
        return list;
    }

    @Override
    public void removeByFilesIds(List<Long> filesIds) {
        if (filesIds == null || filesIds.isEmpty()) return;
        LambdaQueryWrapper<FilesTags> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(FilesTags::getFilesId, filesIds);
        boolean success = this.remove(lambdaQueryWrapper);
        if (!success) {
            log.error("系列或书籍没有绑定标签");
        }
    }

    @Override
    public List<FilesDetailsItemDTO.FilesDetailsTag> saveDataByFilesId(Long filesId, List<FilesDetailsItemDTO.FilesDetailsTag> list) {
        LambdaQueryWrapper<FilesTags> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FilesTags::getFilesId, filesId);
        List<FilesTags> filesTagsList = this.list(lambdaQueryWrapper);

        Set<Integer> tagIds = list.stream().map(item -> item.getTagId()).collect(Collectors.toSet());
        List<Integer> deleteIds = filesTagsList.stream().filter(item -> !tagIds.contains(item.getTagsId())).map(item -> item.getId()).collect(Collectors.toList());
        if (!deleteIds.isEmpty()) if (!this.removeByIds(deleteIds)) throw new BizException("4000", "删除绑定标签失败");

        List<Tags> addTags = list.stream().filter(item -> item.getId() == -1).map(item -> {
            Tags tags = new Tags();
            tags.setName(item.getName());
            return tags;
        }).collect(Collectors.toList());
        if (!addTags.isEmpty()) {
            addTags = tagsService.createTags(addTags);
            Map<String, Integer> tagMap = addTags.stream().collect(Collectors.toMap(Tags::getName, Tags::getId));
            list.stream().filter(item -> item.getId() == -1).forEach(item -> {
                if (tagMap.containsKey(item.getName())) item.setTagId(tagMap.get(item.getName()));
            });
        }

        Set<Integer> hasTagsIds = filesTagsList.stream().map(item -> item.getTagsId()).collect(Collectors.toSet());
        List<FilesTags> addFilesTags = list.stream().filter(item -> !hasTagsIds.contains(item.getTagId())).map(item -> {
            FilesTags f = new FilesTags();
            f.setFilesId(filesId);
            f.setTagsId(item.getTagId());
            return f;
        }).collect(Collectors.toList());
        if (!addFilesTags.isEmpty()) {
            addFilesTags = createList(addFilesTags);
            Map<Integer, Integer> fileTagMap = addFilesTags.stream().collect(Collectors.toMap(FilesTags::getTagsId, FilesTags::getId));
            list.forEach(item -> {
                if (fileTagMap.containsKey(item.getTagId())) {
                    item.setId(fileTagMap.get(item.getTagId()));
                }
            });
        }
        return list;
    }
}