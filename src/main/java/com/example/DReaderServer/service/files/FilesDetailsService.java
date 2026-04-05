package com.example.DReaderServer.service.files;

import com.example.DReaderServer.dto.PageVO;
import com.example.DReaderServer.dto.files.*;
import com.example.DReaderServer.entity.files.FilesDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public interface FilesDetailsService {
    List<FilesDetails> createData(List<FilesDetails> list);

    FilesDetails updateData(FilesDetails filesDetails);

    FilesDetailsItemDTO updateData(FilesDetailsItemDTO filesDetails);

    List<FilesDetails> getOriginalNameList();

    void removeByFilesIds(List<Long> filesIds);

    PageVO<FilesDetailsListDTO> getList(FilesDetailsListQueryCondition filesDetailsListQueryCondition);

    FilesDetailsListDTO getRecent();

    FilesDetailsListDTO randomData();

    FilesDetailsItemDTO getDetails(Long filesId);

    Date updateLastReadTime(Integer id);

    void updateLove(Integer id, Integer love);

    int updateStatus(Long filesId, String lastReadTime);

    String changeCover(Integer id, MultipartFile file);

    String changeCover(Integer id,Integer childId);

    FilesOverviewDTO getOverview();

    List<FilesDetails> updateFolderCover(List<FilesDetails> list);
}
