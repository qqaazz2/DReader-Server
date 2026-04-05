package com.example.DReaderServer.task.utils;

import com.example.DReaderServer.common.TaskInterruptedException;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.storage.FileAdapterService;
import com.example.DReaderServer.util.FilesUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;

@Slf4j
@AllArgsConstructor
public class GetBookCoverTask implements Callable<FilesDetails> {
    Files files;
    FilesUtils filesUtils;
    FileAdapterService fileAdapterService;

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
