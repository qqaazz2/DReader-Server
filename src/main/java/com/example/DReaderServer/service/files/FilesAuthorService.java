package com.example.DReaderServer.service.files;

import com.example.DReaderServer.dto.files.FilesDetailsItemDTO;
import com.example.DReaderServer.entity.files.FilesAuthor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FilesAuthorService {
    List<FilesAuthor> createList(List<FilesAuthor> list);

    void removeByFilesIds(List<Integer> filesIds);

    List<FilesDetailsItemDTO.FilesDetailsAuthor> saveDataByFilesId(Integer filesId,List<FilesDetailsItemDTO.FilesDetailsAuthor> list);
}
