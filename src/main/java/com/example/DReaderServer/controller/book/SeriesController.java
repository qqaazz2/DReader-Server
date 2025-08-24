package com.example.DReaderServer.controller.book;

import com.example.DReaderServer.common.ResultResponse;
import com.example.DReaderServer.dto.book.SeriesListDTO;
import com.example.DReaderServer.dto.book.SeriesListQueryCondition;
import com.example.DReaderServer.dto.book.group.SpecificCheck;
import com.example.DReaderServer.entity.book.Series;
import com.example.DReaderServer.service.book.SeriesService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Update;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/series")
public class SeriesController {
    @Resource
    SeriesService seriesService;

    @GetMapping("/getList")
    public ResultResponse getList(SeriesListQueryCondition seriesListQueryCondition) {
        Set<String> allowSortFields = Set.of("name", "lastReadTime", "createTime");
        Set<String> allowSortOrders = Set.of("ASC", "DESC");
        String sortField = seriesListQueryCondition.getSortField();
        String sortOrder = seriesListQueryCondition.getSortOrder();
        if (sortField != null && !sortField.isBlank()) {
            if (!allowSortFields.contains(sortField)) {
                seriesListQueryCondition.setSortField("name");
            }
            if (sortOrder == null || !allowSortOrders.contains(sortOrder.toUpperCase())) {
                seriesListQueryCondition.setSortOrder("DESC");
            }
        }
        System.out.println(seriesListQueryCondition.getSortField());
        return ResultResponse.success(seriesService.getList(seriesListQueryCondition));
    }

    @GetMapping("/updateLove")
    public ResultResponse updateLove(@Validated({SpecificCheck.class}) SeriesListQueryCondition seriesListQueryCondition) {
        seriesService.updateLove(seriesListQueryCondition.getId(), seriesListQueryCondition.getLove());
        return ResultResponse.success();
    }

    @PostMapping("/updateData")
    public ResultResponse updateData(@Validated({Update.class}) @RequestBody Series series) {
        seriesService.updateData(series);
        return ResultResponse.success();
    }

    @GetMapping("/getDetails")
    public ResultResponse getDetails(@RequestParam(required = true) Integer id) {
        SeriesListDTO seriesListDTO = seriesService.getDetails(id);
        return ResultResponse.success(seriesListDTO);
    }

    @GetMapping("/updateCover")
    public ResultResponse updateCover(@RequestParam Integer id, String cover) {
        seriesService.updateCover(id, cover);
        return ResultResponse.success();
    }

    @GetMapping("/updateLastReadTime")
    public ResultResponse updateLastReadTime(@RequestParam Integer id) {
        return ResultResponse.success(seriesService.updateLastReadTime(id),"更新最后阅读时间成功");
    }

    @GetMapping("/getIdByFilesId")
    public ResultResponse getIdByFilesId(@RequestParam Integer id) {
        return ResultResponse.success(seriesService.getIdByFilesId(id));
    }

    @GetMapping("/randomData")
    public ResultResponse randomData() {
        return ResultResponse.success(seriesService.randomData());
    }
}
