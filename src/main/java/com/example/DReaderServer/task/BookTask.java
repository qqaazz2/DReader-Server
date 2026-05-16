package com.example.DReaderServer.task;

import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.common.TaskInterruptedException;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.entity.book.Book;
import com.example.DReaderServer.service.files.FilesAuthorService;
import com.example.DReaderServer.service.files.FilesDetailsService;
import com.example.DReaderServer.service.files.FilesService;
import com.example.DReaderServer.service.book.BookService;
import com.example.DReaderServer.service.book.SeriesService;
import com.example.DReaderServer.storage.FileAdapterFactory;
import com.example.DReaderServer.storage.FileAdapterService;
import com.example.DReaderServer.task.utils.GetBookCoverTask;
import com.example.DReaderServer.task.utils.ScanContext;
import com.example.DReaderServer.util.FilesUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Async
@Slf4j
@Component
public class BookTask extends AsyncTask {
    @Resource
    BookService bookService;

    @Resource
    FilesDetailsService filesDetailsService;

    @Resource
    FilesService filesService;

    @Resource
    FileAdapterFactory fileAdapterFactory;

    @Resource
    RedisTemplate redisTemplate;

    private FileAdapterService fileAdapterService;

    public BookTask() {
        basePath = "books";
    }

    @Override
    @Transactional
    public void create(ScanContext scanContext) {
        fileAdapterService = fileAdapterFactory.getFileAdapter();
        List<Book> bookList = new ArrayList<>();
        List<FilesDetails> filesDetailsList = new ArrayList<>();
        Map<Long, Integer> filesDetailsMap = new HashMap<>();
        checkInterrupted();
        List<Future<FilesDetails>> futureList = new ArrayList<>();
        ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(10000));;
        List<Files> createList = scanContext.getCreateFiles();
        createList = filesService.createFiles(createList);
        int folderCount = 0;
        for (Files files : createList) {
            checkInterrupted();
            if (files.getIsFolder() == 2) {
                checkInterrupted(() -> executor.shutdown());
                Future<FilesDetails> future = executor.submit(new GetBookCoverTask(files, filesUtils, fileAdapterService));
                futureList.add(future);
                folderCount++;
            } else {
                FilesDetails filesDetails = new FilesDetails();
                filesDetails.setFilesId(files.getId());
                filesDetails.setName(files.getFileName());
                filesDetails.setIsFolder(1);
                filesDetailsList.add(filesDetails);
                filesDetailsMap.put(files.getId(), filesDetailsList.size() == 0 ? 0 : filesDetailsList.size() - 1);
            }
        }
        executor.shutdown();

        checkInterrupted();
        for (Future<FilesDetails> future : futureList) {
            checkInterrupted();
            try {
                FilesDetails details = future.get();
                if (filesDetailsMap.containsKey(details.getParentId())) {
                    Integer folderDetailsIndex = filesDetailsMap.get(details.getParentId());
                    filesDetailsList.get(folderDetailsIndex).setCover(details.getCover());
                    filesDetailsMap.remove(details.getParentId());
                }
                filesDetailsList.add(details);
                Book book = new Book();
                book.setFilesId(details.getFilesId());
                bookList.add(book);
            } catch (InterruptedException e) {
                checkInterrupted();
                Thread.currentThread().interrupt();
            } catch (TaskInterruptedException e) {
                checkInterrupted();
            } catch (Exception e) {
                e.printStackTrace();
                executor.shutdownNow();
                future.cancel(true);
                throw new BizException("4000", "EPUB文件识别失败，请重试");
            }
        }
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }

        if (filesDetailsList.size() > 0) filesDetailsService.createData(filesDetailsList);
        if (bookList.size() > 0) bookService.createData(bookList);
        log.info("本次扫描共新增{}本书籍，{}个系列", bookList.size(), folderCount);
        for (FilesDetails filesDetails : filesDetailsList) {
            redisTemplate.opsForList().rightPush("scrape_queue", filesDetails);
        }
    }
}