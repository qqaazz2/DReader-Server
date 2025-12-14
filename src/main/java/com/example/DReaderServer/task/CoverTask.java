package com.example.DReaderServer.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.common.TaskInterruptedException;
import com.example.DReaderServer.dto.bgm.*;
import com.example.DReaderServer.dto.files.FilesCoverChangeDTO;
import com.example.DReaderServer.entity.Author;
import com.example.DReaderServer.entity.Tags;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.files.FilesAuthor;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.entity.files.FilesTags;
import com.example.DReaderServer.mapper.AuthorMapper;
import com.example.DReaderServer.mapper.TagsMapper;
import com.example.DReaderServer.mapper.files.FilesDetailsMapper;
import com.example.DReaderServer.mapper.files.FilesMapper;
import com.example.DReaderServer.service.AuthorService;
import com.example.DReaderServer.service.TagsService;
import com.example.DReaderServer.service.files.FilesAuthorService;
import com.example.DReaderServer.service.files.FilesDetailsService;
import com.example.DReaderServer.service.files.FilesTagsService;
import com.example.DReaderServer.service.impl.files.FilesDetailsServiceImpl;
import com.example.DReaderServer.storage.FileAdapterFactory;
import com.example.DReaderServer.storage.FileAdapterService;
import com.example.DReaderServer.util.FilesUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CoverTask extends BaseTask {
    @Value("${file.upload}")
    String filePath;

    @Resource
    FilesDetailsMapper filesDetailsMapper;

    @Resource
    FilesDetailsService filesDetailsService;

    @Resource
    FileAdapterFactory fileAdapterFactory;

    public void start(long currentGeneration) {
        if (check(currentGeneration)) return;
        FileAdapterService fileAdapterService = fileAdapterFactory.getFileAdapter();
        Set<String> filesSet = fileAdapterService.getFileList(filePath + "bookCover");

        List<FilesCoverChangeDTO> list = filesDetailsMapper.getCoverList();
        Set<String> removeSet = list.stream().filter(filesSet::contains).map(FilesCoverChangeDTO::getCover).collect(Collectors.toSet());
        List<FilesCoverChangeDTO> dbHasNotFile = list.stream().filter(item -> !removeSet.contains(item.getCover())).collect(Collectors.toList());
        Map<Integer, Integer> folderIdMap = new HashMap<>();
        if (check(currentGeneration)) return;
        ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(10000));
        try {
            for (FilesCoverChangeDTO filesCoverChangeDTO : dbHasNotFile) {
                if (check(currentGeneration)) {
                    executor.shutdownNow();
                    return;
                }
                if (filesCoverChangeDTO.getIsFolder() == 1) {
                    folderIdMap.put(filesCoverChangeDTO.getFilesId(), filesCoverChangeDTO.getId());
                    continue;
                }
                executor.submit(new GetCoverTask(filesCoverChangeDTO, fileAdapterService));
            }
            executor.shutdown();
            while (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                if (check(currentGeneration)) {
                    executor.shutdownNow();
                    return;
                }
            }
            if (check(currentGeneration)) return;
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            return;
        }

        Set<Integer> folderKeys = folderIdMap.keySet();
        List<FilesDetails> folders = list.stream().filter(item -> folderKeys.contains(item.getParentId()) && item.getSort() == 0).map(item -> {
            FilesDetails filesDetails = new FilesDetails();
            filesDetails.setId(folderIdMap.get(item.getParentId()));
            filesDetails.setCover(item.getCover());
            return filesDetails;
        }).collect(Collectors.toList());
        filesDetailsService.updateFolderCover(folders);
        if (check(currentGeneration)) return;
        filesSet.removeAll(removeSet);
        fileAdapterService.removeByList(filesSet);
    }

    private boolean check(long currentGeneration) {
        boolean success = currentGeneration != activeGeneration.get() || !running.get();
        if (success) log.info("发现新的封面处理任务启动，结束当前的任务");
        return success;
    }
}

@Slf4j
@AllArgsConstructor
class GetCoverTask implements Runnable {
    FilesCoverChangeDTO files;
    FileAdapterService fileAdapterService;

    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
        File file = new File(files.getFilePath());
        if (!file.exists()) throw new RuntimeException("文件不存在");
        try (InputStream inputStream = new FileInputStream(file)) {
            EpubReader epubReader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = epubReader.readEpub(inputStream);
            if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
            if (epubBook.getCoverImage() == null) throw new RuntimeException("书籍中获取图片失败");
            fileAdapterService.upload(epubBook.getCoverImage().getData(), files.getCover(), "image/jpeg");
        } catch (TaskInterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("书籍{}扫描失败：{}", files.getName(), e.getMessage());
        }
    }
}