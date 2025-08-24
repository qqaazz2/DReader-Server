package com.example.DReaderServer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.dto.setting.ProportionDTO;
import com.example.DReaderServer.dto.setting.TimeCountDTO;
import com.example.DReaderServer.entity.Files;
import com.example.DReaderServer.mapper.FilesMapper;
import com.example.DReaderServer.service.FilesService;
import com.example.DReaderServer.util.ValidationUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Override
    public List<Files> getByType(Integer type) {
        LambdaQueryWrapper<Files> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Files::getType, type);
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
        System.out.println(files);
        if (!isTrue) throw new BizException("4000", "创建文件数据失败");
        return files;
    }

    @Override
    public void removerFiles(List<Files> files) {
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
        lambdaQueryWrapper.eq(files.getType() != null, Files::getType, files.getType());
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
            Integer type = files.getType();
            Integer count = files.getCount();
            dtoList.add(new ProportionDTO(type, count));
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
        if(files.getFileSize() == null){
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
        queryWrapper.ne(Files::getType, 0);
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
}