package io.github.qqaazz2.DReaderServer.service.files;

import io.github.qqaazz2.DReaderServer.dto.setting.ProportionDTO;
import io.github.qqaazz2.DReaderServer.dto.setting.TimeCountDTO;
import io.github.qqaazz2.DReaderServer.entity.files.Files;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface FilesService {

    List<Files> getFilesList();

    List<Files> renameFiles(List<Files> files);

    List<Files> createFiles(List<Files> files);

    void removerFiles(List<Files> files);

    void rename(Integer id,String name);

    Files getFiles(Files files);

    Files createFile(Files files);

    List<ProportionDTO> filesProportion();

    Map<String,Object> getFilesCount();

    List<TimeCountDTO> getYearMonth();
}
