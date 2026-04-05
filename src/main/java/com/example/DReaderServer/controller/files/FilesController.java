package com.example.DReaderServer.controller.files;

import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.example.DReaderServer.common.ResultResponse;
//import com.example.DReaderServer.entity.book.Series;
import com.example.DReaderServer.dto.files.FilesDetailsItemDTO;
import com.example.DReaderServer.dto.files.FilesDetailsListQueryCondition;
import com.example.DReaderServer.dto.files.group.SpecificCheck;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.service.book.SeriesService;
import com.example.DReaderServer.service.files.FilesDetailsService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class FilesController {
    @Resource
    FilesDetailsService filesDetailsService;

    @GetMapping("/getList")
    public ResultResponse getList(FilesDetailsListQueryCondition queryCondition) {
        return ResultResponse.success(filesDetailsService.getList(queryCondition));
    }

    @GetMapping("/updateLove")
    public ResultResponse updateLove(@RequestParam int id, @RequestParam int love) {
        filesDetailsService.updateLove(id, love);
        return ResultResponse.success();
    }

    @GetMapping("/getRecent")
    public ResultResponse getRecent(){
        return ResultResponse.success(filesDetailsService.getRecent());
    }

    @PostMapping("/updateData")
    public ResultResponse updateData(@Validated({Update.class}) @RequestBody FilesDetailsItemDTO filesDetailsItemDTO) {
        return ResultResponse.success(filesDetailsService.updateData(filesDetailsItemDTO));
    }

    @GetMapping("/getDetails")
    public ResultResponse getDetails(@RequestParam Long id) {
        return ResultResponse.success(filesDetailsService.getDetails(id));
    }

    //
    @GetMapping("/updateCover")
    public ResultResponse updateCover(@RequestParam Integer id, @RequestParam Integer childId) {
        return ResultResponse.success(filesDetailsService.changeCover(id, childId));
    }

    @PostMapping("/changeCover")
    public ResultResponse changeCover(@RequestParam MultipartFile file, @RequestParam Integer id) {
        return ResultResponse.success(filesDetailsService.changeCover(id, file));
    }

    @GetMapping("/updateStatus")
    public ResultResponse updateStatus(@RequestParam Long id, @RequestParam String lastReadTime) {
        return ResultResponse.success(filesDetailsService.updateStatus(id, lastReadTime), "更新阅读状态及最后阅读时间成功");
    }

    @GetMapping("/getOverview")
    public ResultResponse getOverview() {
        return ResultResponse.success(filesDetailsService.getOverview());
    }

//
//    @GetMapping("/getIdByFilesId")
//    public ResultResponse getIdByFilesId(@RequestParam Integer id) {
//        return ResultResponse.success(seriesService.getIdByFilesId(id));
//    }
//
    @GetMapping("/randomData")
    public ResultResponse randomData() {
        return ResultResponse.success(filesDetailsService.randomData());
    }
}
