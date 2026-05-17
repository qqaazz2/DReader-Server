package io.github.qqaazz2.DReaderServer.service.files;

import io.github.qqaazz2.DReaderServer.dto.PageVO;
import io.github.qqaazz2.DReaderServer.dto.files.*;
import io.github.qqaazz2.DReaderServer.dto.files.*;
import io.github.qqaazz2.DReaderServer.entity.files.FilesDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

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

    void updateStatusByField(UpdateStatusRequestDTO updateStatusRequestDTO);
}
