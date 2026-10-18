package io.github.qqaazz2.DReaderServer.task.scraper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.config.WebClientConfig;
import io.github.qqaazz2.DReaderServer.entity.LoginUser;
import io.github.qqaazz2.DReaderServer.entity.User;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperPerson;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperSubject;
import io.github.qqaazz2.DReaderServer.enums.ExceptionEnum;
import io.github.qqaazz2.DReaderServer.mapper.UserMapper;
import io.github.qqaazz2.DReaderServer.mapper.scraper.ScraperSubjectMapper;
import io.github.qqaazz2.DReaderServer.service.TokenService;
import io.github.qqaazz2.DReaderServer.service.scraper.ScraperSubjectService;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterFactory;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class BgmArchiveScraper extends BaseScraper {
    @Resource
    WebClientConfig webClient;

    @Resource
    ScraperSubjectService bgmSubjectService;

    @Resource
    ScraperSubjectMapper bgmSubjectMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    FileAdapterFactory factory;

    @Resource
    TokenService tokenService;

    @Value("${file.upload}")
    String filePath;

    private static final Object LOCK = new Object();
    private static volatile boolean init = false;

    @Override
    public List<ScraperSubject> getOriginalDataList(String name) {
        getArchiveZip();
        return bgmSubjectMapper.search(name);
    }

    @Override
    public ScraperSubject getOriginalDataByUserSelectMatchId(Integer id) {
        getArchiveZip();
        return bgmSubjectMapper.selectById(id);
    }

    private void getArchiveZip() {
        if (init) return;
        synchronized (LOCK) {
            try {
                if (init) return;
                ArchiveAUXResponse response = webClient.githubWebClient().get()
                        .uri("https://raw.githubusercontent.com/bangumi/Archive/master/aux/latest.json")
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(10))
                        .retry(3)
                        .map(json -> {
                            try {
                                return objectMapper.readValue(json, ArchiveAUXResponse.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .block();
                if (check(response.id)) {
                    init = true;
                    return;
                }
                Path jsonPath = Path.of(filePath + "/json");
                if (!Files.exists(jsonPath)) {
                    Files.createDirectories(jsonPath);
                }
                Path downloadPath = Paths.get(filePath + "/json/" + response.id + ".zip");
                recreateTable();

                log.info("开始下载：{}", downloadPath);
                webClient.githubWebClient().get() .uri(response.browserDownloadUrl) .retrieve() .bodyToFlux(DataBuffer.class) .as(flux -> DataBufferUtils.write( flux, downloadPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING )) .doOnError(e -> log.error(e.toString())) .block();
                log.info("下载完成：{}，存在：{}，大小：{}", downloadPath, Files.exists(downloadPath), Files.size(downloadPath));

                unzip(downloadPath);
                handleSubjectData();
                Files.walk(jsonPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                changeArchiveId(response.id);
                init = true;
            } catch (Exception exception) {
                throw new RuntimeException(exception.getMessage());
            }
        }
    }

    private void recreateTable() {
        bgmSubjectMapper.dropTable();
        bgmSubjectMapper.createTable();
    }

    private void unzip(Path downloadPath) {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(downloadPath))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                Files.copy(
                        zipInputStream,
                        Path.of(filePath + "/json/" + zipEntry.getName()),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
            zipInputStream.closeEntry();
            Files.delete(downloadPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleSubjectData() {
        Queue<ScraperSubject> bgmSubjectDataQueue = new ArrayDeque<>();
        Map<Integer, List<Integer>> personBySubject = new HashMap<>();
        Map<Integer, ScraperPerson> personDataMap = new HashMap<>();
        File jsonFolder = new File(filePath + "/json");
        for (File file : jsonFolder.listFiles()) {
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                String line;
                ScraperSubject subjectData;
                ScraperPerson personData;
                SubjectPerson subjectPerson;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    if (file.getName().equals("subject.jsonlines")) {
                        subjectData = objectMapper.readValue(line, ScraperSubject.class);
                        if (subjectData.getType() != 1) continue;
                        bgmSubjectDataQueue.offer(subjectData);
                    }

                    if (file.getName().equals("person.jsonlines")) {
                        personData = objectMapper.readValue(line, ScraperPerson.class);
                        personDataMap.put(personData.getId(), personData);
                    }

                    if (file.getName().equals("subject-persons.jsonlines")) {
                        subjectPerson = objectMapper.readValue(line, SubjectPerson.class);
                        List<Integer> list = personBySubject.get(subjectPerson.getSubjectId());
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                        list.add(subjectPerson.getPersonId());
                        personBySubject.put(subjectPerson.getSubjectId(), list);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage() + e.getStackTrace());
            }
        }

        try {
            List<ScraperSubject> batch = new ArrayList<>(1000);
            while (!bgmSubjectDataQueue.isEmpty()) {
                ScraperSubject subject = bgmSubjectDataQueue.poll();
                List<Integer> personIds = personBySubject.get(subject.getId());
                if (personIds != null) {
                    for (Integer personId : personIds) {
                        ScraperPerson personData = personDataMap.get(personId);
                        if (personData != null) {
                            subject.getPersons().add(personData);
                        }
                    }
                }
                batch.add(subject);
                if (batch.size() >= 1000) {
                    bgmSubjectService.createDataAll(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                bgmSubjectService.createDataAll(batch);
            }
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    private boolean check(String id) {
       User user = userMapper.selectById(1);
       if(user.getArchiveId() == null) return false;
       return user.getArchiveId().equals(id);
    }

    @Override
    public void close() {

    }

    @Override
    public String getName() {
        return "Bangumi Archive";
    }

    @Override
    public String getAuthorAvatarPath(Integer authorId) {
        FileAdapterService fileAdapterService = factory.getFileAdapter();
        byte[] imageBytes = webClient.bgmWebClient().get().uri("https://api.bgm.tv/v0/persons/" + authorId + "/image?type=large").retrieve().bodyToMono(byte[].class).timeout(Duration.ofSeconds(10)).retry(3).block();
        if (imageBytes != null) {
            String path = fileAdapterService.uploadSplicing(imageBytes, "/authorAvatar/" + authorId + ".jpg", "image/jpeg");
            imageBytes = null;
            return path;
        }
        return null;
    }

    public void changeArchiveId(String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            User user = loginUser.getUser();
            LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(User::getId, user.getId());
            lambdaUpdateWrapper.set(User::getArchiveId, id);
            if (userMapper.update(lambdaUpdateWrapper) == 0) throw new BizException("4000", "修改Bangumi Archive版本失败");
            user.setArchiveId(id);
            tokenService.updateToken(user);
        } else {
            throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR);
        }
    }
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class ArchiveAUXResponse {
    @JsonProperty("browser_download_url")
    String browserDownloadUrl;
    @JsonProperty("id")
    String id;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SubjectPerson {
    @JsonProperty("person_id")
    Integer personId;
    @JsonProperty("subject_id")
    Integer subjectId;
}
