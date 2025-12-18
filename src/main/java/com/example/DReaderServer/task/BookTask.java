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

    @Resource
    ObjectMapper objectMapper;
    public Map<String, String> coverMap = new ConcurrentHashMap<>();
    public List<Files> epubList = new ArrayList<>();
    public List<Book> bookList = new ArrayList<>();
    public Map<Integer, FilesDetails> filesDetailsMap = new HashMap<>();
    public List<FilesDetails> filesDetailsList = new ArrayList<>();
    public Map<Integer, Integer> updateSeries = new HashMap<>();
    public List<Files> folderList = new ArrayList<>();
    public int folderCount = 0;
    private FileAdapterService fileAdapterService;

    public BookTask() {
        basePath = "books";
        contentType = 1;
    }

    @Override
    @Transactional
    public void create() {
        epubList.clear();
        folderList.clear();
        bookList.clear();
        filesDetailsMap.clear();
        updateSeries.clear();
        filesDetailsList.clear();

        fileAdapterService = fileAdapterFactory.getFileAdapter();
        if (createFiles.size() == 0) return;
        checkInterrupted();
        deepCreate(createFiles, 1);
        List<Future<FilesDetails>> futureList = new ArrayList<>();
        ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(10000));
        for (Files files : epubList) {
            checkInterrupted(() -> executor.shutdownNow());
            Future<FilesDetails> future = executor.submit(new GetBookCoverTask(files, filesUtils, fileAdapterService, this.coverMap));
            futureList.add(future);
        }
        checkInterrupted();
        for (Future<FilesDetails> future : futureList) {
            checkInterrupted();
            try {
                processing(future.get());
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

//        List<Integer> list = new ArrayList<>();
//        for (FilesDetails filesDetails : filesDetailsList) {
//            checkInterrupted();
//            String coverPath = null;
//            if (coverMap.containsKey(filesDetails.getHash())) {
//                coverPath = coverMap.get(filesDetails.getHash());
//                book.setCover(coverPath);
//            }
//
//            int parentId = book.getParentId();
//            if (!list.contains(book.getParentId()) && seriesMap.containsKey(parentId)) {
//                Series series = seriesMap.get(parentId);
//                if (coverPath != null) {
//                    series.setCover(coverPath);
//                }
//                series.setAuthor(book.getAuthor());
//                series.setProfile(book.getProfile());
//                seriesMap.put(parentId, series);
//                list.add(parentId);
//            }
//        }

        if (filesDetailsList.size() > 0) filesDetailsService.createData(filesDetailsList);
        if (bookList.size() > 0) bookService.createData(bookList);
        log.info("本次共扫描{}本书籍，{}个系列", bookList.size(), folderCount);
        for (FilesDetails filesDetails : filesDetailsList) {
            redisTemplate.opsForList().rightPush("scrape_queue", filesDetails);
        }
    }

    public void processing(FilesDetails details) {
        if (filesDetailsMap.containsKey(details.getParentId())) {
            FilesDetails folderDetails = filesDetailsMap.get(details.getParentId());
            folderDetails.setCover(details.getCover());
            filesDetailsMap.remove(details.getParentId());
            filesDetailsList.add(folderDetails);
        }

        filesDetailsList.add(details);
        Book book = new Book();
        book.setFilesId(details.getFilesId());
        bookList.add(book);
    }

    public void deepCreate(List<Files> list, Integer index) {
        checkInterrupted();
        folderCount++;
        list = filesService.createFiles(list);
        for (Files files : list) {
            checkInterrupted();
            Boolean isTrue = files.getFileName().substring(files.getFileName().lastIndexOf(".") + 1).toLowerCase().equals("epub");

            if (files.getIsFolder() == 1) {
                FilesDetails filesDetails = new FilesDetails();
                filesDetails.setFilesId(files.getId());
                filesDetails.setName(files.getFileName());
                filesDetails.setIsFolder(1);
                filesDetailsMap.put(files.getId(), filesDetails);
            } else if (files.getIsFolder() == 2 && isTrue) {
                epubList.add(files);
            } else continue;

            if (files.getIsFolder() == 2 || files.getChild() == null || files.getChild().size() == 0) continue;
            List<Files> childes = files.getChild().stream().peek(value -> value.setParentId(files.getId())).toList();
            deepCreate(childes, index += 1);
        }
    }
}

@Slf4j
@AllArgsConstructor
class GetBookCoverTask implements Callable<FilesDetails> {
    Files files;
    FilesUtils filesUtils;
    FileAdapterService fileAdapterService;
    Map<String, String> coverMap;

    @Override
    public FilesDetails call() {
        FilesDetails filesDetails = new FilesDetails();
        if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
        try (InputStream inputStream = new FileInputStream(files.getFile())) {
            EpubReader epubReader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = epubReader.readEpub(inputStream);

            Metadata metadata = epubBook.getMetadata();
//            StringBuilder stringBuilder = new StringBuilder();
//            for (Author author : metadata.getAuthors()) {
//                stringBuilder.append(author.getFirstname() + author.getLastname());
//            }
//            book.setAuthor(stringBuilder.toString());
//            StringBuilder publisherBuilder = new StringBuilder();
//            for (String publisher : metadata.getPublishers()) {
//                publisherBuilder.append(publisher);
//            }
            if (metadata.getDescriptions().size() > 0) filesDetails.setProfile(metadata.getDescriptions().get(0));
            filesDetails.setFilesId(files.getId());
            filesDetails.setHash(files.getHash());
            filesDetails.setParentId(files.getParentId());
            filesDetails.setIsFolder(2);
            filesDetails.setName(files.getFileName());
            try {
                if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
                if (epubBook.getCoverImage() == null) return filesDetails;
                byte[] data = epubBook.getCoverImage().getData();
                String cover = "bookCover" + File.separator + files.getHash() + ".jpg";
                cover = fileAdapterService.uploadSplicing(data, cover, "image/jpeg");
                filesDetails.setCover(cover);
                data = null;
            } catch (TaskInterruptedException e) {
                throw e;
            } catch (Exception e) {
                log.error("{}封面获取失败：", files.getFileName(), e.getMessage());
                return filesDetails;
            }
            if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
        } catch (TaskInterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("书籍{}扫描失败：{}", files.getFileName(), e.getMessage());
        }
        return filesDetails;
    }
}


