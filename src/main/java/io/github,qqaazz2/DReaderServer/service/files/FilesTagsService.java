package io.github.qqaazz2.DReaderServer.service.files;

import io.github.qqaazz2.DReaderServer.dto.files.FilesDetailsItemDTO;
import io.github.qqaazz2.DReaderServer.entity.files.FilesTags;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FilesTagsService {
    List<FilesTags> createList(List<FilesTags> list);

    void removeByFilesIds(List<Long> filesIds);

    List<FilesDetailsItemDTO.FilesDetailsTag> saveDataByFilesId(Long filesId, List<FilesDetailsItemDTO.FilesDetailsTag> list);
}
