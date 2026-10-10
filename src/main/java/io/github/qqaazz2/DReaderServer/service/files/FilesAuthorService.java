package io.github.qqaazz2.DReaderServer.service.files;

import io.github.qqaazz2.DReaderServer.dto.files.FilesDetailsItemDTO;
import io.github.qqaazz2.DReaderServer.entity.files.FilesAuthor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FilesAuthorService {
    List<FilesAuthor> createList(List<FilesAuthor> list);

    void removeByFilesIds(List<Long> filesIds);

    List<FilesDetailsItemDTO.FilesDetailsAuthor> saveDataByFilesId(Long filesId,List<FilesDetailsItemDTO.FilesDetailsAuthor> list);
}
