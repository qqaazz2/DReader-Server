package io.github.qqaazz2.DReaderServer.task;

import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.common.TaskInterruptedException;
import io.github.qqaazz2.DReaderServer.entity.book.Book;
import io.github.qqaazz2.DReaderServer.entity.files.Files;
import io.github.qqaazz2.DReaderServer.entity.files.FilesDetails;
import io.github.qqaazz2.DReaderServer.service.book.BookService;
import io.github.qqaazz2.DReaderServer.service.files.FilesDetailsService;
import io.github.qqaazz2.DReaderServer.service.files.FilesService;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterService;
import io.github.qqaazz2.DReaderServer.task.utils.GetBookCoverTask;
import io.github.qqaazz2.DReaderServer.task.utils.ScanContext;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;

@Async
@Slf4j
@Component
public class BookTask extends io.github.qqaazz2.DReaderServer.task.AsyncTask {
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