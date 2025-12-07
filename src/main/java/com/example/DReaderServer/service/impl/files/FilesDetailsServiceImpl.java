package com.example.DReaderServer.service.impl.files;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.dto.PageVO;
import com.example.DReaderServer.dto.book.SeriesListQueryCondition;
import com.example.DReaderServer.dto.files.*;
import com.example.DReaderServer.entity.book.BookFileCover;
import com.example.DReaderServer.entity.files.FilesAuthor;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.entity.files.FilesTags;
import com.example.DReaderServer.mapper.files.FilesAuthorMapper;
import com.example.DReaderServer.mapper.files.FilesDetailsMapper;
import com.example.DReaderServer.mapper.files.FilesTagsMapper;
import com.example.DReaderServer.service.files.FilesAuthorService;
import com.example.DReaderServer.service.files.FilesDetailsService;
import com.example.DReaderServer.service.files.FilesTagsService;
import com.example.DReaderServer.storage.FileAdapterFactory;
import com.example.DReaderServer.storage.FileAdapterService;
import com.example.DReaderServer.util.FileTypeUtils;
import jakarta.annotation.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FilesDetailsServiceImpl extends ServiceImpl<FilesDetailsMapper, FilesDetails> implements FilesDetailsService {

    @Resource
    FilesDetailsMapper filesDetailsMapper;

    @Resource
    FilesAuthorMapper filesAuthorMapper;

    @Resource
    FilesTagsMapper filesTagsMapper;

    @Resource
    FilesTagsService filesTagsService;

    @Resource
    FilesAuthorService filesAuthorService;

    @Resource
    FileAdapterFactory factory;

    @Override
    public List<FilesDetails> createData(List<FilesDetails> list) {
        Boolean isTrue = this.saveBatch(list);
        if (!isTrue) throw new BizException("4000", "新增文件信息失败");
        return list;
    }

    @Override
    public FilesDetails updateData(FilesDetails filesDetails) {
        Boolean isTrue = this.updateById(filesDetails);
        if (!isTrue) throw new BizException("4000", "编辑文件信息失败");
        return filesDetails;
    }

    @Override
    public FilesDetailsItemDTO updateData(FilesDetailsItemDTO filesDetailsItemDTO) {
        FilesDetails filesDetails = new FilesDetails();
        BeanUtils.copyProperties(filesDetailsItemDTO, filesDetails);
        Boolean isTrue = this.updateById(filesDetails);
        if (!isTrue) throw new BizException("4000", "编辑文件信息失败");
        filesDetailsItemDTO.setFilesTags(filesTagsService.saveDataByFilesId(filesDetails.getFilesId(), filesDetailsItemDTO.getFilesTags()));
        filesDetailsItemDTO.setFilesAuthors(filesAuthorService.saveDataByFilesId(filesDetails.getFilesId(), filesDetailsItemDTO.getFilesAuthors()));
        return filesDetailsItemDTO;
    }

    @Override
    public List<FilesDetails> getOriginalNameList() {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.select("id", "files_id", "original_name");
        return this.list(queryWrapper);
    }

    @Override
    public void removeByFilesIds(List<Integer> filesIds) {
        if (filesIds == null || filesIds.isEmpty()) return;
        LambdaQueryWrapper<FilesDetails> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(FilesDetails::getFilesId, filesIds);
        boolean success = this.remove(lambdaQueryWrapper);
        if (!success) {
            throw new BizException("4000", "删除文件信息失败");
        }
    }

    @Override
    public PageVO<FilesDetailsListDTO> getList(FilesDetailsListQueryCondition queryCondition) {
        Set<String> allowSortFields = Set.of("name", "lastReadTime", "createTime");
        Set<String> allowSortOrders = Set.of("ASC", "DESC");
        String sortField = queryCondition.getSortField();
        String sortOrder = queryCondition.getSortOrder();
        if (sortField != null && !sortField.isBlank()) {
            if (!allowSortFields.contains(sortField)) {
                queryCondition.setSortField("name");
            }
            if (sortOrder == null || !allowSortOrders.contains(sortOrder.toUpperCase())) {
                queryCondition.setSortOrder("DESC");
            }
        }
        List<FilesDetailsListDTO> list = filesDetailsMapper.getList(queryCondition);
        Integer count = filesDetailsMapper.count(queryCondition);
        return new PageVO(queryCondition.getLimit(), queryCondition.getPage(), count, list);
    }

    @Override
    public FilesDetailsListDTO getRecent() {
        FilesDetailsListDTO filesDetailsListDTO = filesDetailsMapper.getRecent();
        return filesDetailsListDTO;
    }

    @Override
    public FilesDetailsItemDTO getDetails(Integer id) {
        FilesDetailsItemDTO filesDetailsItemDTO = filesDetailsMapper.getOne(id);
        if (filesDetailsItemDTO == null) throw new BizException("4000", "查询不到对应的文件数据");
        filesDetailsItemDTO.setFilesAuthors(filesAuthorMapper.getAuthorByFilesId(filesDetailsItemDTO.getFilesId()));
        filesDetailsItemDTO.setFilesTags(filesTagsMapper.getTagsByFilesId(filesDetailsItemDTO.getFilesId()));
        return filesDetailsItemDTO;
    }

    @Override
    public Date updateLastReadTime(Integer id) {
        Date date = new Date();
        LambdaUpdateWrapper<FilesDetails> updateWrapper = new LambdaUpdateWrapper();
        updateWrapper.eq(FilesDetails::getId, id).set(FilesDetails::getLastReadTime, date);
        if (!this.update(updateWrapper)) throw new BizException("4000", "更新最后一次阅读时间失败");
        return date;
    }

    @Override
    public void updateLove(Integer id, Integer love) {
        LambdaUpdateWrapper<FilesDetails> updateWrapper = new LambdaUpdateWrapper();
        updateWrapper.eq(FilesDetails::getId, id).set(FilesDetails::getLove, love);
        Boolean isTrue = this.update(updateWrapper);
        if (!isTrue) throw new BizException("4000", "修改喜欢状态失败");
    }

    @Override
    public int updateStatus(int filesId, String lastReadTime) {
        List<Integer> statuses = filesDetailsMapper.getStatusByParentId(filesId);
        if (statuses.isEmpty()) throw new BizException("4000", "未查询到该系列下的子文件");
        int status;
        long countUnread = statuses.stream().filter(s -> s == 1).count(); // 连载
        long countOver = statuses.stream().filter(s -> s == 2).count();
        if (countUnread == statuses.size()) {
            status = 1;
        } else if (countOver == statuses.size()) {
            status = 2;
        } else {
            status = 3;
        }
        LambdaUpdateWrapper<FilesDetails> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FilesDetails::getFilesId, filesId)
                .set(FilesDetails::getLastReadTime, lastReadTime)
                .set(FilesDetails::getStatus, status);
        if (!this.update(wrapper)) {
            throw new BizException("4000", "更新系列阅读状态失败");
        }

        return status;
    }

    @Override
    public String changeCover(Integer id, MultipartFile multipartFile) {
        FileTypeUtils.validateFile(multipartFile, new String[]{"jpg"}, 10240);
        try {
            String newCoverPath = "bookCover" + File.separator + DigestUtils.md5Hex(String.valueOf(LocalDateTime.now())) + ".jpg";
            LambdaUpdateWrapper<FilesDetails> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(FilesDetails::getId, id);
            newCoverPath = factory.getFileAdapter().upload(multipartFile.getBytes(), newCoverPath, "image/jpeg");
            updateWrapper.set(FilesDetails::getCover, newCoverPath);
            if (!this.update(updateWrapper)) throw new BizException();
            return newCoverPath;
        } catch (Exception e) {
            throw new BizException("4000", "修改书籍封面图片失败");
        }
    }

    @Override
    public String changeCover(Integer id, Integer childId) {
        LambdaQueryWrapper<FilesDetails> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FilesDetails::getId, childId);
        FilesDetails filesDetails = this.getOne(lambdaQueryWrapper);
        if (filesDetails == null) throw new BizException("4000", "找不到文件信息数据");

        LambdaUpdateWrapper<FilesDetails> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(FilesDetails::getId, id);
        lambdaUpdateWrapper.set(FilesDetails::getCover, filesDetails.getCover());
        if (!this.update(lambdaUpdateWrapper)) throw new BizException("4000", "修改系列封面图片失败");

        return filesDetails.getCover();
    }

    @Override
    public FilesOverviewDTO getOverview() {
        List<FilesOverviewListDTO> list = filesDetailsMapper.getOverview();
        FilesOverviewDTO dto = new FilesOverviewDTO();
        for (FilesOverviewListDTO item : list) {
            boolean isFolder = item.getIsFolder() == 1;
            boolean isLove   = item.getLove() == 2;
            if (isFolder) {
                dto.seriesCount++;
                if (isLove) dto.loveSeriesCount++;

                switch (item.getOverStatus()) {
                    case 1 -> dto.unOverSeriesCount++;
                    case 2 -> dto.overSeriesCount++;
                    case 3 -> dto.discardedSeriesCount++;
                }
            } else {
                dto.bookCount++;
                if (isLove) dto.loveBookCount++;
                switch (item.getStatus()) {
                    case 1 -> dto.unreadCount++;
                    case 2 -> dto.overCount++;
                    case 3 -> dto.readingCount++;
                }
            }
        }
        return dto;
    }
}