package com.example.DReaderServer.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.example.DReaderServer.common.BizException;
import com.example.DReaderServer.dto.bgm.*;
import com.example.DReaderServer.dto.files.FilesDetailsItemDTO;
import com.example.DReaderServer.entity.Author;
import com.example.DReaderServer.entity.Tags;
import com.example.DReaderServer.entity.files.Files;
import com.example.DReaderServer.entity.files.FilesAuthor;
import com.example.DReaderServer.entity.files.FilesDetails;
import com.example.DReaderServer.entity.files.FilesTags;
import com.example.DReaderServer.mapper.AuthorMapper;
import com.example.DReaderServer.mapper.TagsMapper;
import com.example.DReaderServer.mapper.files.FilesAuthorMapper;
import com.example.DReaderServer.mapper.files.FilesDetailsMapper;
import com.example.DReaderServer.mapper.files.FilesMapper;
import com.example.DReaderServer.mapper.files.FilesTagsMapper;
import com.example.DReaderServer.service.AuthorService;
import com.example.DReaderServer.service.TagsService;
import com.example.DReaderServer.service.files.FilesAuthorService;
import com.example.DReaderServer.service.files.FilesDetailsService;
import com.example.DReaderServer.service.files.FilesTagsService;
import com.example.DReaderServer.storage.FileAdapterFactory;
import com.example.DReaderServer.storage.FileAdapterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScrapeTask extends BaseTask {
    @Resource
    RedisTemplate redisTemplate;

    @Resource
    WebClient webClient;

    @Resource
    TagsMapper tagsMapper;

    @Resource
    AuthorMapper authorMapper;

    @Resource
    TagsService tagsService;

    @Resource
    AuthorService authorService;

    @Resource
    FilesAuthorService filesAuthorService;

    @Resource
    FilesTagsService filesTagsService;

    @Resource
    FilesDetailsService filesDetailsService;

    @Resource
    FilesMapper filesMapper;

    @Resource
    FilesAuthorMapper filesAuthorMapper;

    @Resource
    FilesTagsMapper filesTagsMapper;

    @Resource
    FileAdapterFactory factory;

    private Map<String, Integer> tagsMap;
    private Map<String, Integer> authorMap;
    private String tagKey = "tag:";
    private String authorKey = "author:";
    private List<Integer> filesIds = new ArrayList<>();
    private List<String> relationList = List.of("书系", "出版社", "作者", "插图");
    private Map<Integer, String> folderOriginalNameMap = new HashMap<>();
    private Map<Integer, Map<String, Integer>> seriesSubjects = new HashMap<>();
    private Map<Integer, List<Integer>> authorFilesMap = new HashMap<>();
    private Map<Integer, List<Integer>> tagFilesMap = new HashMap<>();
    private String platformType = "小说";
    private static final Pattern VOLUME_PATTERN = Pattern.compile("\\((\\d+)\\)");
    private FileAdapterService fileAdapterService;

    public void start(long currentGeneration) {
        fileAdapterService = factory.getFileAdapter();
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.select("id");
        List<Files> list = filesMapper.selectList(queryWrapper);
        filesIds = list.stream().map(item -> item.getId()).collect(Collectors.toList());
        tagFilesMap = filesTagsMapper.selectList(new QueryWrapper<>())
                .stream()
                .collect(Collectors.groupingBy(
                        FilesTags::getFilesId,
                        Collectors.mapping(FilesTags::getTagsId, Collectors.toList())
                ));
        authorFilesMap = filesAuthorMapper.selectList(new QueryWrapper<>())
                .stream()
                .collect(Collectors.groupingBy(
                        FilesAuthor::getFilesId,
                        Collectors.mapping(FilesAuthor::getAuthorId, Collectors.toList())
                ));
        tagsMap = tagsMapper.selectList(new LambdaQueryWrapper<>()).stream().collect(Collectors.toMap(Tags::getName, Tags::getId));
        authorMap = authorMapper.selectList(new LambdaQueryWrapper<>()).stream().collect(Collectors.toMap(Author::getName, Author::getId));
        folderOriginalNameMap = filesDetailsService.getOriginalNameList().stream().filter(item -> item.getOriginalName() != null).collect(Collectors.toMap(FilesDetails::getFilesId, FilesDetails::getOriginalName));
        while (true) {
            if (currentGeneration != activeGeneration.get() || !running.get()) {
                log.info("发现新的刮削任务启动，结束当前的任务");
                return;
            }
            FilesDetails filesDetails = (FilesDetails) redisTemplate.opsForList().leftPop("scrape_queue");
            if (filesDetails == null) {
                log.info("刮削队列为空，刮削任务结束");
                break;
            }
            if (!filesIds.contains(filesDetails.getFilesId())) continue;
            try {
                BgmSubjectDTO bgmSubjectDTO = startScrapeWorker(filesDetails);
                Thread.sleep(1000);
                processing(filesDetails, bgmSubjectDTO);
            } catch (Exception exception) {
                log.error(exception.getMessage());
                redisTemplate.opsForList().rightPush("scrape_queue_fail", filesDetails);
            }
        }
    }

    public void processing(FilesDetails filesDetails, BgmSubjectDTO bgmSubjectDTO) {
        filesDetails.setBgmId(bgmSubjectDTO.getId());
        filesDetails.setProfile(bgmSubjectDTO.getSummary());
        filesDetails.setOriginalName(bgmSubjectDTO.getName());
        filesDetails.setDate(bgmSubjectDTO.getDate());
        if (filesDetails.getIsFolder() == 1 && !folderOriginalNameMap.containsKey(filesDetails.getFilesId()))
            folderOriginalNameMap.put(filesDetails.getFilesId(), bgmSubjectDTO.getName());
        filesDetailsService.updateData(filesDetails);
        getAuthor(filesDetails, bgmSubjectDTO);
        getTags(filesDetails, bgmSubjectDTO);
    }

    public BgmSubjectDTO startScrapeWorker(FilesDetails filesDetails) throws JsonProcessingException, InterruptedException {
        BgmSubjectDTO bgmSubjectDTO = null;
        BgmSearchFilterDTO bgmSearchFilterDTO = new BgmSearchFilterDTO();
        bgmSearchFilterDTO.setNsfw(true);
        bgmSearchFilterDTO.setType(List.of(1));
        String searchName = getSearchName(filesDetails);

        BgmSearchDTO bgmSearchDTO = new BgmSearchDTO();
        bgmSearchDTO.setSort("rank");
        bgmSearchDTO.setKeyword(searchName);
        bgmSearchDTO.setFilter(bgmSearchFilterDTO);

        if (filesDetails.getIsFolder() == 2) {
            bgmSubjectDTO = processBook(filesDetails);
            if (bgmSubjectDTO != null) return bgmSubjectDTO;
        }
        SearchSubjectsResponse response = webClient.post().uri("/v0/search/subjects?limit=10&offset=0").bodyValue(bgmSearchDTO).retrieve().bodyToMono(SearchSubjectsResponse.class).timeout(Duration.ofSeconds(10)).retry(3).doOnError(err -> log.error("BGM网络请求失败: {}", err.getMessage())).block();
        List<BgmSubjectDTO> subjectDTOList = (response != null && response.getData() != null) ? response.getData() : null;
        if (subjectDTOList == null || subjectDTOList.isEmpty())
            throw new BizException("4000", filesDetails.getName() + "找不到对应条目");
        if (filesDetails.getIsFolder() == 1) {
            for (BgmSubjectDTO item : subjectDTOList) {
                if (item.getPlatform().equals(platformType) && item.getSeries()) {
                    bgmSubjectDTO = item;
                    break;
                }
            }

            if (bgmSubjectDTO == null || bgmSubjectDTO.getId() == null)
                throw new BizException("4000", filesDetails.getName() + " 搜索到了条目，但未找到匹配的系列");
            folderOriginalNameMap.put(filesDetails.getFilesId(), searchName);
            Thread.sleep(1000);

            List<BgmSubjectDTO> bgmSubjectDTOList = webClient.get().uri("/v0/subjects/{id}/subjects", bgmSubjectDTO.getId()).retrieve().bodyToFlux(BgmSubjectDTO.class).timeout(Duration.ofSeconds(10)).retry(3).doOnError(err -> log.error("BGM网络请求失败: {}", err.getMessage())).collectList().block();
            if (!bgmSubjectDTOList.isEmpty()) {
                Map<String, Integer> map = bgmSubjectDTOList.stream().filter(item -> item.getRelation().equals("单行本")).collect(Collectors.toMap(BgmSubjectDTO::getName, BgmSubjectDTO::getId));
                seriesSubjects.put(filesDetails.getFilesId(), map);
            }

            return bgmSubjectDTO;
        } else {
            return subjectDTOList.get(0);
        }
    }

    public BgmSubjectDTO processBook(FilesDetails filesDetails) {
        Map<String, Integer> map = seriesSubjects.get(filesDetails.getParentId());
        if (map == null || map.isEmpty()) return null;
        String fileName = filesDetails.getName();
        String fileVolNumStr = fileName.replaceAll("\\D+", "");
        if (fileVolNumStr.isEmpty()) {
            log.warn("文件名 {} 中未找到数字，无法匹配卷号", fileName);
            return null;
        }

        Integer fileVolNum;
        try {
            fileVolNum = Integer.parseInt(fileVolNumStr);
        } catch (NumberFormatException e) {
            log.warn("从 {} 提取的数字 {} 无法解析为整数", fileName, fileVolNumStr);
            return null;
        }

        // 4. 遍历 Map，匹配卷号
        Integer matchedBgmId = null;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String bgmName = entry.getKey();
            Matcher matcher = VOLUME_PATTERN.matcher(bgmName.trim());
            if (matcher.find()) {
                try {
                    Integer bgmVolNum = Integer.parseInt(matcher.group(1));
                    if (fileVolNum.equals(bgmVolNum)) {
                        matchedBgmId = entry.getValue();
                        log.info("文件 {} 匹配成功: BGM 条目 '{}', ID: {}", fileName, bgmName, matchedBgmId);
                        break;
                    }
                } catch (NumberFormatException e) {
                    log.warn("解析 BGM 条目 '{}' 的卷号失败", bgmName);
                }
            }
        }

        if (matchedBgmId == null) {
            log.warn("文件 {} (卷号 {}) 未在系列中匹配到BGM条目", fileName, fileVolNum);
            return null; // 返回 null，让 startScrapeWorker 去搜索
        }
        BgmSubjectDTO bgmSubjectDTO = webClient.get().uri("/v0/subjects/{id}", matchedBgmId).retrieve().bodyToMono(BgmSubjectDTO.class).timeout(Duration.ofSeconds(10)).retry(3).doOnError(err -> log.error("BGM网络请求失败: {}", err.getMessage())).block();
        return bgmSubjectDTO;
    }

    public String getSearchName(FilesDetails filesDetails) {
        String searchName = filesDetails.getName();
        String originalName = folderOriginalNameMap.get(filesDetails.getParentId());
        if (originalName == null) return filesDetails.getName();

        StringBuilder stringBuilder = new StringBuilder().append(originalName);
        if (filesDetails.getIsFolder() == 1) {
            stringBuilder.append(" ").append(searchName);
        } else {
            stringBuilder.append(filesDetails.getName().replaceAll("\\D+", ""));
        }
        return stringBuilder.toString();
    }

    private void getTags(FilesDetails filesDetails, BgmSubjectDTO bgmSubjectDTO) {
        List<FilesTags> filesTagsList = new ArrayList<>();
        for (BgmSubjectDTO.SubjectTag subjectTag : bgmSubjectDTO.getTags()) {
            FilesTags filesTags = new FilesTags();
            filesTags.setFilesId(filesDetails.getFilesId());
            if (tagsMap.containsKey(subjectTag.getName())) {
                filesTags.setTagsId(tagsMap.get(subjectTag.getName()));
                if (tagFilesMap.containsKey(filesDetails.getFilesId()) && tagFilesMap.get(filesDetails.getFilesId()).contains(tagsMap.get(subjectTag.getName())))
                    return;
            } else {
                Tags tags = tagsService.createTags(subjectTag.getName());
                tagsMap.put(subjectTag.getName(), tags.getId());
                filesTags.setTagsId(tags.getId());
            }
            filesTagsList.add(filesTags);
        }
        filesTagsService.createList(filesTagsList);
    }

    private void getAuthor(FilesDetails filesDetails, BgmSubjectDTO bgmSubjectDTO) {
        List<FilesAuthor> filesAuthorsList = new ArrayList<>();
        List<BgmPersonsDTO> bgmPersonsDTOList = webClient.get().uri("/v0/subjects/{id}/persons", bgmSubjectDTO.getId()).retrieve().bodyToFlux(BgmPersonsDTO.class).timeout(Duration.ofSeconds(10)).retry(3).doOnError(err -> log.error("BGM网络请求失败: {}", err.getMessage())).collectList().block();
        if (bgmPersonsDTOList == null || bgmPersonsDTOList.size() == 0) return;
        List<Integer> ids = new ArrayList<>();
        for (BgmPersonsDTO bgmPersonsDTO : bgmPersonsDTOList) {
            Integer id = null;
            FilesAuthor filesAuthor = new FilesAuthor();
            filesAuthor.setFilesId(filesDetails.getFilesId());
            if (!relationList.contains(bgmPersonsDTO.getRelation())) continue;
            id = authorMap.get(bgmPersonsDTO.getName());
            if (ids.contains(id)) continue;
            if (id == null) {
                Author author = new Author();
                bgmPersonsDTO = webClient.get().uri("/v0/persons/{id}", bgmPersonsDTO.getId()).retrieve().bodyToMono(BgmPersonsDTO.class).timeout(Duration.ofSeconds(10)).retry(3).doOnError(err -> log.error("BGM网络请求失败: {}", err.getMessage())).block();
                if (bgmPersonsDTO == null) continue;
                byte[] imageBytes = webClient.get().uri(bgmPersonsDTO.getImages().get("large")).retrieve().bodyToMono(byte[].class).timeout(Duration.ofSeconds(10)).retry(3).block();
                if (imageBytes != null) {
                    author.setAvatar(fileAdapterService.uploadSplicing(imageBytes, "/authorAvatar/" + bgmPersonsDTO.getId() + ".jpg", "image/jpeg"));
                    imageBytes = null;
                }
                author.setName(bgmPersonsDTO.getName());
                author.setVocational(bgmPersonsDTO.getRelation());
                author.setBgmId(bgmPersonsDTO.getId());
                author.setProfile(bgmPersonsDTO.getSummary());
                if (bgmPersonsDTO.getBirthYear() != null && bgmPersonsDTO.getBirthMon() != null && bgmPersonsDTO.getBirthDay() != null)
                    author.setDate(LocalDate.of(bgmPersonsDTO.getBirthYear(), bgmPersonsDTO.getBirthMon(), bgmPersonsDTO.getBirthDay()));
                authorService.createAuthor(author);
                authorMap.put(bgmPersonsDTO.getName(), author.getId());
                id = author.getId();
            }
            if (authorFilesMap.containsKey(filesDetails.getFilesId()) && authorFilesMap.get(filesDetails.getFilesId()).contains(id))
                return;
            ids.add(id);
            filesAuthor.setAuthorId(id);
            filesAuthorsList.add(filesAuthor);
        }
        filesAuthorService.createList(filesAuthorsList);
    }
}
