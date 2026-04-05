package com.example.DReaderServer.service.files;

import com.example.DReaderServer.dto.files.FilesDetailsItemDTO;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.entity.files.FilesTags;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FilesTagsService {
    List<FilesTags> createList(List<FilesTags> list);

    void removeByFilesIds(List<Long> filesIds);

    List<FilesDetailsItemDTO.FilesDetailsTag> saveDataByFilesId(Long filesId, List<FilesDetailsItemDTO.FilesDetailsTag> list);
}
