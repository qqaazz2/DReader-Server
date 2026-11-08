package io.github.qqaazz2.DReaderServer.service.impl.files;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.qqaazz2.DReaderServer.service.files.FilesAuthorService;
import io.github.qqaazz2.DReaderServer.service.files.FilesDetailsService;
import io.github.qqaazz2.DReaderServer.service.files.FilesService;
import io.github.qqaazz2.DReaderServer.service.files.FilesTagsService;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.dto.setting.ProportionDTO;
import io.github.qqaazz2.DReaderServer.dto.setting.TimeCountDTO;
import io.github.qqaazz2.DReaderServer.entity.files.Files;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesMapper;
import io.github.qqaazz2.DReaderServer.util.ValidationUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FilesServiceImpl extends ServiceImpl<FilesMapper, Files> implements FilesService {

    @Value("${file.upload}")
    String filePath;

    @Resource
    FilesMapper filesMapper;

    @Resource
    FilesAuthorService filesAuthorService;

    @Resource
    FilesDetailsService filesDetailsService;

    @Resource
    FilesTagsService filesTagsService;


    @Override
    public List<Files> getFilesList() {
        LambdaQueryWrapper<Files> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        return this.list(lambdaQueryWrapper);
    }

    @Override
    public List<Files> renameFiles(List<Files> files) {
        this.updateBatchById(files);
        return files;
    }

    @Override
    public List<Files> createFiles(List<Files> files) {
        boolean isTrue = this.saveBatch(files);
        if (!isTrue) throw new BizException("4000", "创建文件数据失败");
        return files;
    }

    @Override
    public void removerFiles(List<Files> files) {
        List<Long> filesIds = files.stream().map(item -> item.getId()).collect(Collectors.toList());
        filesAuthorService.removeByFilesIds(filesIds);
        filesDetailsService.removeByFilesIds(filesIds);
        filesTagsService.removeByFilesIds(filesIds);
        this.removeBatchByIds(files);
    }

    @Override
    public void rename(Integer id, String name) {
        LambdaUpdateWrapper<Files> updateWrapper = new LambdaUpdateWrapper<Files>();
        updateWrapper.eq(Files::getId, id).set(Files::getFileName, name);
        ValidationUtils.checkCondition(this.update(updateWrapper), "文件重命名失败");
    }

    @Override
    public Files getFiles(Files files) {
        LambdaQueryWrapper<Files> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(files.getIsFolder() != null, Files::getIsFolder, files.getIsFolder());
        lambdaQueryWrapper.eq(files.getParentId() != null, Files::getParentId, files.getParentId());
        lambdaQueryWrapper.eq(!files.getFileName().isEmpty(), Files::getFileName, files.getFileName());
        return this.getOne(lambdaQueryWrapper);
    }

    @Override
    public Files createFile(Files files) {
        boolean isTrue = this.save(files);
        if (!isTrue) throw new BizException("4000", "创建文件数据失败");
        return files;
    }

    @Override
    public List<ProportionDTO> filesProportion() {
        QueryWrapper<Files> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 1) // deleted = 1
                .eq("is_folder", 2) // is_folder = 2
                .select("type", "COUNT(type) as count") // 选择字段，COUNT(type) 给它别名为 count
                .groupBy("type"); // 按 type 分组
        List<Files> list = filesMapper.selectList(queryWrapper);

        List<ProportionDTO> dtoList = new ArrayList<>();
        for (Files files : list) {
            Integer count = files.getCount();
            dtoList.add(new ProportionDTO(1, count));
        }

        return dtoList;
    }

    @Override
    public Map<String, Object> getFilesCount() {
        QueryWrapper<Files> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_folder", 2);
        queryWrapper.select("COUNT(*) as count", "SUM(file_size) as fileSize");
        Files files = this.getOne(queryWrapper);

        Map<String, Object> map = new HashMap<>();
        if (files.getFileSize() == null) {
            map.put("count", files.getCount());
            map.put("size", 0);
            return map;
        }
        BigDecimal byteValue = new BigDecimal(files.getFileSize());
        BigDecimal divisor = new BigDecimal("1024").multiply(new BigDecimal("1024")).multiply(new BigDecimal("1024"));
        BigDecimal result = byteValue.divide(divisor, 2, RoundingMode.HALF_UP);  // 保留4位小数，四舍五入
        map.put("count", files.getCount());
        map.put("size", result.doubleValue());
        return map;
    }

    @Override
    public List<TimeCountDTO> getYearMonth() {

        LambdaQueryWrapper<Files> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Files::getIsFolder, 2);
        queryWrapper.select(Files::getAdd_time);
        List<LocalDateTime> list = this.listObjs(queryWrapper, obj -> (LocalDateTime) obj);
        List<Date> dateList = list.stream()
                .map(localDateTime -> Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                .collect(Collectors.toList());
        return getCount(dateList);
    }

    public List<TimeCountDTO> getCount(List<Date> list) {
        Map<Integer, TimeCountDTO> map = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        List<TimeCountDTO> timeCountDTOList = list.stream().collect(Collectors.groupingBy(value -> {
            cal.setTime(value);
            return cal.get(Calendar.YEAR);
        }, Collectors.groupingBy(
                value -> {
                    cal.setTime(value);
                    return cal.get(Calendar.MONTH) + 1; // 按月份分组，月份从1开始
                },
                Collectors.counting() // 计数
        ))).entrySet().stream().map(entry -> {
            Integer year = entry.getKey();
            Integer count = 0;
            Map<Integer, Long> monthCounts = entry.getValue();

            List<TimeCountDTO> timeCountDTOS = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                TimeCountDTO timeCountDTO = new TimeCountDTO();
                timeCountDTO.setTime(i);
                if (monthCounts.containsKey(i)) {
                    timeCountDTO.setCount(Math.toIntExact(monthCounts.get(i)));
                    count += Math.toIntExact(monthCounts.get(i));
                } else {
                    timeCountDTO.setCount(0);
                }
                timeCountDTOS.add(timeCountDTO);
            }

            TimeCountDTO timeCountDTO = new TimeCountDTO();
            timeCountDTO.setTime(year);
            timeCountDTO.setCount(count);
            timeCountDTO.setChildren(timeCountDTOS);

            return timeCountDTO;
        }).toList();

        return timeCountDTOList;
    }

//    @Override
//    public ResponseEntity<StreamingResponseBody> download(Long id, String range) {
//        try {
//            Files files = this.getById(id);
//            if (files == null) throw new BizException("4000", "文件不存在");
//            Path path = Paths.get(files.getFilePath());
//            if (!java.nio.file.Files.exists(path) || !java.nio.file.Files.isRegularFile(path))
//                throw new BizException("4000", "文件路径错误");
//            long fileSize = java.nio.file.Files.size(path);
//            long start = 0, end = fileSize - 1;
//            try {
//                if (range != null || range.startsWith("bytes=")) {
//                    String[] ranges = range.substring(6).split("-", -1);
//                    if (ranges.length != 2 || ranges[0].isBlank()) throw new BizException("4000", "文件断点下载失败");
//                    start = Long.parseLong(ranges[0]);
//                    if (!ranges[1].isBlank()) end = Long.parseLong(ranges[1]);
//                    if (start < 0 || start >= fileSize || end < fileSize || end >= fileSize)
//                        throw new BizException("4000", "文件断点下载失败");
//                }
//            } catch (RuntimeException e) {
//                HttpHeaders headers = new HttpHeaders();
//                headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
//                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(headers).build();
//            }
//            final long fileStart = start;
//            long contentLength = fileSize - 1;
//            boolean isRange = range != null;
//            StreamingResponseBody body = outputStream -> {
//                try (RandomAccessFile randomAccessFile = new RandomAccessFile(files.getFilePath(), "r")) {
//                    randomAccessFile.seek(fileStart);
//                    byte[] buffer = new byte[8096];
//                    int len;
//                    long remaining = contentLength;
//                    while (remaining > 0 && (len = randomAccessFile.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
//                        outputStream.write(buffer, 0, len);
//                        remaining -= len;
//                    }
//                }
//            };
//            HttpHeaders headers = new HttpHeaders();
//            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
//            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.set(HttpHeaders.ETAG, files.getHash());
//            headers.setContentLength(contentLength);
//            if (isRange) headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
//            return ResponseEntity.status(isRange ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK).headers(headers).body(body);
//        } catch (RuntimeException | IOException e) {
//            throw new BizException("4000", e.getMessage());
//        }
//    }

    @Override
    public ResponseEntity<StreamingResponseBody> download(Long id, String range) {
        try {
            Files files = this.getById(id);
            if (files == null) throw new BizException("4000", "文件不存在");
            Path path = Paths.get(files.getFilePath());
            if (!java.nio.file.Files.exists(path) || !java.nio.file.Files.isRegularFile(path)) throw new BizException("4000", "文件路径错误");
            long fileSize = java.nio.file.Files.size(path);
            StreamingResponseBody body = outputStream -> {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(files.getFilePath(), "r")) {
                    byte[] buffer = new byte[8096];
                    int len;
                    long remaining = fileSize;
                    while (remaining > 0 && (len = randomAccessFile.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        outputStream.write(buffer, 0, len);
                        remaining -= len;
                    }
                }
            };
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.ETAG, files.getHash());
            headers.setContentLength(fileSize);
            return ResponseEntity.status(HttpStatus.OK).headers(headers).body(body);
        } catch (RuntimeException | IOException e) {
            throw new BizException("4000", e.getMessage());
        }
    }
}