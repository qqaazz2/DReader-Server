package io.github.qqaazz2.DReaderServer.task;

import io.github.qqaazz2.DReaderServer.common.TaskInterruptedException;
import io.github.qqaazz2.DReaderServer.entity.files.FilesDetails;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesDetailsMapper;
import io.github.qqaazz2.DReaderServer.service.files.FilesDetailsService;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterService;
import io.github.qqaazz2.DReaderServer.dto.files.FilesCoverChangeDTO;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterFactory;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.epub.EpubReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CoverTask extends io.github.qqaazz2.DReaderServer.task.BaseTask {
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
        Map<Long, Integer> folderIdMap = new HashMap<>();
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

        Set<Long> folderKeys = folderIdMap.keySet();
        List<FilesDetails> folders = list.stream().filter(item -> folderKeys.contains(item.getParentId()) && item.getSort() == 0).map(item -> {
            FilesDetails filesDetails = new FilesDetails();
            filesDetails.setId(folderIdMap.get(item.getParentId()));
            filesDetails.setCover(item.getCover());
            return filesDetails;
        }).collect(Collectors.toList());
        if(folders.size() > 0)filesDetailsService.updateFolderCover(folders);
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