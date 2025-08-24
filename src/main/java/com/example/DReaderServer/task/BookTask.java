package com.example.DReaderServer.task;

import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.common.TaskInterruptedException;
import com.example.DReaderServer.entity.Files;
import com.example.DReaderServer.entity.book.Book;
import com.example.DReaderServer.entity.book.Series;
import com.example.DReaderServer.service.FilesService;
import com.example.DReaderServer.service.UploadService;
import com.example.DReaderServer.service.book.BookService;
import com.example.DReaderServer.service.book.SeriesService;
import com.example.DReaderServer.util.FilesUtils;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
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
    SeriesService seriesService;

    @Resource
    FilesService filesService;

    @Resource
    UploadService uploadService;


    public Map<String, String> coverMap = new ConcurrentHashMap<>();
    public List<Files> epubList = new ArrayList<>();
    public List<Book> bookList = new ArrayList<>();
    public Map<Integer, Series> seriesMap = new HashMap<>();
    public Map<Integer, Integer> updateSeries = new HashMap<>();
    public List<Files> folderList = new ArrayList<>();

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
        seriesMap.clear();
        updateSeries.clear();

        if (createFiles.size() == 0) return;
        checkInterrupted();
        deepCreate(createFiles, 1);
        setSeries();
        List<Future<Book>> futureList = new ArrayList<>();
        ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(5000));
        for (Files files : epubList) {
            checkInterrupted(() -> executor.shutdownNow());
            Future<Book> future = executor.submit(new GetBookCoverTask(files, filesUtils, uploadService, this.coverMap));
            futureList.add(future);
        }
        checkInterrupted();
        for (Future<Book> future : futureList) {
            checkInterrupted();
            try {
                Book book = future.get();
                bookList.add(book);
            } catch (InterruptedException e) {
                checkInterrupted();
                Thread.currentThread().interrupt();
            } catch (TaskInterruptedException e) {
                checkInterrupted();
            } catch (Exception e) {
                System.out.println("error");
                e.printStackTrace();
                executor.shutdownNow();
                future.cancel(true);
                throw new BizException("4000", "EPUB文件识别失败，请重试");
            }
        }
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }

        List<Integer> list = new ArrayList<>();
        for (Book book : bookList) {
            checkInterrupted();
            String coverPath = null;
            if (coverMap.containsKey(book.getHash())) {
                coverPath = coverMap.get(book.getHash());
                book.setCover(coverPath);
            }

            int parentId = book.getParentId();
            if (!list.contains(book.getParentId()) && seriesMap.containsKey(parentId)) {
                Series series = seriesMap.get(parentId);
                if (coverPath != null) {
                    series.setCover(coverPath);
                }
                series.setAuthor(book.getAuthor());
                series.setProfile(book.getProfile());
                seriesMap.put(parentId, series);
                list.add(parentId);
            }
        }

        if (seriesMap.size() > 0) seriesService.createData(seriesMap.values().stream().toList());
        if (bookList.size() > 0) bookService.createData(bookList);
        log.info("本次共扫描{}本书籍，{}个系列", bookList.size(), seriesMap.size());
        updateSeries.forEach((key, value) -> {
            checkInterrupted();
            seriesService.updateNum(key, value);
        });
    }

    public void deepCreate(List<Files> list, Integer index) {
        checkInterrupted();
        list = filesService.createFiles(list);
        for (Files files : list) {
            checkInterrupted();
            if (files.getIsFolder() == 1) folderList.add(files);
            Boolean isTrue = files.getFileName().substring(files.getFileName().lastIndexOf(".") + 1).toLowerCase().equals("epub");
            if (files.getIsFolder() == 2 && isTrue) {
                epubList.add(files);
                if (files.getParentId() != -1 && index == 1) {
                    Integer num = files.getFile().getParentFile().list().length;
                    updateSeries.put(files.getParentId(), num);
                }
            }

            if (files.getChild() == null || files.getChild().size() == 0) continue;
            List<Files> childes = files.getChild().stream().peek(value -> value.setParentId(files.getId())).toList();
            deepCreate(childes, index += 1);
        }
    }

    public void setSeries() {
        for (Files files : folderList) {
            Series series = new Series();
            series.setFilesId(files.getId());
            series.setFilesId(files.getId());
            series.setNum(files.getFileSize().intValue());
            seriesMap.put(files.getId(), series);
            checkInterrupted();
        }
    }
}

@Slf4j
@AllArgsConstructor
class GetBookCoverTask implements Callable<Book> {
    Files files;
    FilesUtils filesUtils;
    UploadService uploadService;
    Map<String, String> coverMap;

    @Override
    public Book call() {
        Book book = new Book();
        if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
        try (InputStream inputStream = new FileInputStream(files.getFile())) {
            EpubReader epubReader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = epubReader.readEpub(inputStream);

            Metadata metadata = epubBook.getMetadata();
            StringBuilder stringBuilder = new StringBuilder();
            for (Author author : metadata.getAuthors()) {
                stringBuilder.append(author.getFirstname() + author.getLastname());
            }
            book.setAuthor(stringBuilder.toString());
            StringBuilder publisherBuilder = new StringBuilder();
            for (String publisher : metadata.getPublishers()) {
                publisherBuilder.append(publisher);
            }
            book.setPublishing(publisherBuilder.toString());
            if (metadata.getDescriptions().size() > 0) book.setProfile(metadata.getDescriptions().get(0));
            book.setFilesId(files.getId());
            book.setHash(files.getHash());
            book.setParentId(files.getParentId());
            try {
                if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
                if (epubBook.getCoverImage() == null) return book;
                byte[] data = epubBook.getCoverImage().getData();
                String cover = files.getFile().getParent() + File.separator + files.getHash() + ".jpg";
                cover = uploadService.upload(data, cover, "image/jpeg");
                coverMap.put(files.getHash(), cover);
            } catch (TaskInterruptedException e) {
                throw e;
            } catch (Exception e) {
                log.error("{}封面获取失败：", files.getFileName(), e.getMessage());
                return book;
            }
            if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
        } catch (TaskInterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("书籍{}扫描失败：{}", files.getFileName(), e.getMessage());
            e.printStackTrace();
        }
        return book;
    }
}


