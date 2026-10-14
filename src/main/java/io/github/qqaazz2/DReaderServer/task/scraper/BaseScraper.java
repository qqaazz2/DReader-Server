package io.github.qqaazz2.DReaderServer.task.scraper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.entity.Author;
import io.github.qqaazz2.DReaderServer.entity.LoginUser;
import io.github.qqaazz2.DReaderServer.entity.Tags;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperPerson;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperSubject;
import io.github.qqaazz2.DReaderServer.entity.files.FilesAuthor;
import io.github.qqaazz2.DReaderServer.entity.files.FilesDetails;
import io.github.qqaazz2.DReaderServer.entity.files.FilesTags;
import io.github.qqaazz2.DReaderServer.enums.ExceptionEnum;
import io.github.qqaazz2.DReaderServer.mapper.AuthorMapper;
import io.github.qqaazz2.DReaderServer.mapper.TagsMapper;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesAuthorMapper;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesDetailsMapper;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesMapper;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesTagsMapper;
import io.github.qqaazz2.DReaderServer.service.AuthorService;
import io.github.qqaazz2.DReaderServer.service.TagsService;
import io.github.qqaazz2.DReaderServer.service.files.FilesAuthorService;
import io.github.qqaazz2.DReaderServer.service.files.FilesDetailsService;
import io.github.qqaazz2.DReaderServer.service.files.FilesTagsService;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterFactory;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterService;
import io.github.qqaazz2.DReaderServer.task.BaseTask;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public abstract class BaseScraper extends BaseTask {

    @Resource
    RedisTemplate redisTemplate;

    @Resource
    TagsMapper tagsMapper;

    @Resource
    AuthorMapper authorMapper;

    @Resource
    FilesAuthorMapper filesAuthorMapper;

    @Resource
    FilesTagsMapper filesTagsMapper;

    @Resource
    FilesDetailsMapper filesDetailsMapper;

    @Resource
    FilesAuthorService filesAuthorService;

    @Resource
    FilesTagsService filesTagsService;

    @Resource
    TagsService tagsService;

    @Resource
    AuthorService authorService;
    protected static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TAG_FILES = "scrape:tag_files";
    private static final String AUTHOR_FILES = "scrape:author_files";
    private static final String TAGS = "scrape:tags";
    private static final String AUTHORS = "scrape:authors";
    private static final Map<String, String> CAREER = Map.of(
            "producer", "制作人",
            "mangaka", "漫画家",
            "artist", "画师",
            "seiyu", "声优",
            "writer", "作家",
            "illustrator", "插画师",
            "actor", "演员"
    );
    private boolean init = false;

    public void start(long currentGeneration) {
        try {
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            List<FilesTags> tagsId = new ArrayList<>();
            Map<String, List<Long>> missingTags = new HashMap<>();
            List<FilesAuthor> authorsId = new ArrayList<>();
            Map<String, ScraperPerson> missingAuthors = new HashMap<>();
            Map<String,List<Long>> missingAuthorsByFilesIds = new HashMap<>();
            MatchData matchData;
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
                if (!init) init();
                try {
                    if (filesDetails.getIsFolder() != 1) continue;
                    matchData = match(filesDetails);
                    if (matchData.getType()) {
                        ScraperSubject scraperSubject = matchData.getScraperSubject();
                        filesDetails.setProfile(scraperSubject.getSummary());
                        filesDetails.setBgmId(scraperSubject.getId());
                        filesDetails.setDate(scraperSubject.getDate());
                        filesDetails.setScraper(1);
                        if(scraperSubject.getNameCn() != null){
                            filesDetails.setOriginalName(scraperSubject.getName());
                        }
                        filesDetailsMapper.updateById(filesDetails);

                        Long filesId = filesDetails.getFilesId();
                        List<Integer> fileTagIds = (List<Integer>) redisTemplate.opsForHash().get(TAG_FILES, filesId);
                        if (fileTagIds == null) fileTagIds = new ArrayList<>();
                        List<String> tagNames = matchData.getScraperSubject().getTags().stream().map(item -> item.getName()).toList();
                        List<Object> values = redisTemplate.opsForHash().multiGet(TAGS, new ArrayList<>(tagNames));
                        for (int i = 0; i < tagNames.size(); i++) {
                            Object value = values.get(i);
                            if (value == null) {
                                List<Long> list = missingTags.get(tagNames.get(i));
                                if (list == null) list = new ArrayList<>();
                                list.add(filesId);
                                missingTags.put(tagNames.get(i), list);
                                continue;
                            }

                            Integer tagId = Integer.parseInt(value.toString());
                            if (fileTagIds.contains(tagId)) continue;
                            FilesTags filesTags = new FilesTags();
                            filesTags.setTagsId(tagId);
                            filesTags.setFilesId(filesId);
                            tagsId.add(filesTags);
                        }

                        List<Integer> fileAuthorIds = (List<Integer>) redisTemplate.opsForHash().get(AUTHOR_FILES, filesId);
                        if (fileAuthorIds == null) fileAuthorIds = new ArrayList<>();
                        List<ScraperPerson> persons = matchData.getScraperSubject().getPersons();
                        List<Object> authorValues = redisTemplate.opsForHash().multiGet(AUTHORS, persons.stream().map(ScraperPerson::getName).toList());
                        for (int i = 0; i < persons.size(); i++) {
                            ScraperPerson person = persons.get(i);
                            Object value = authorValues.get(i);
                            if (value == null) {
                                if(!missingAuthors.containsKey(persons.get(i).getName())) missingAuthors.put(persons.get(i).getName(), persons.get(i));
                                List<Long> list = missingAuthorsByFilesIds.get(persons.get(i).getName());
                                if (list == null) list = new ArrayList<>();
                                list.add(filesId);
                                missingAuthorsByFilesIds.put(persons.get(i).getName(), list);
                                continue;
                            }

                            Integer authorId = Integer.parseInt(value.toString());
                            if (fileAuthorIds.contains(authorId)) continue;

                            FilesAuthor filesAuthor = new FilesAuthor();
                            filesAuthor.setAuthorId(authorId);
                            filesAuthor.setFilesId(filesId);

                            authorsId.add(filesAuthor);
                        }

                        if (tagsId.size() >= 1000 || authorsId.size() >= 1000) bindTagsAndPerson(missingTags,missingAuthors,missingAuthorsByFilesIds,authorsId,tagsId);
                    } else {
                        filesDetails.setScraper(2);
                        filesDetailsMapper.updateById(filesDetails);
                    }
                    System.out.println("我要睡觉了，我要睡5秒");
                    Thread.sleep(5000);
                    System.out.println("睡了5秒我起床了");
                } catch (Exception exception) {
                    log.error(exception.getMessage());
                    redisTemplate.opsForList().rightPush("scrape_queue_fail", filesDetails);
                }
            }
            bindTagsAndPerson(missingTags,missingAuthors,missingAuthorsByFilesIds,authorsId,tagsId);
        }catch (Exception exception){
            log.error(exception.getMessage());
        }
    }

    private static final Set<String> CONFLICT_WORDS = Set.of(
            "完结", "重制", "外传", "特别篇", "剧场版", "ova", "oad", "sp", "最终季", "最终章"
    );

    private static final double PASS_SCORE = 0.85;

    public abstract List<ScraperSubject> getOriginalDataList(String name);

    public abstract ScraperSubject getOriginalDataByUserSelectMatchId(Integer id);

    @Override
    public void close() {
    }

    public MatchData match(FilesDetails filesDetails) {
        MatchData matchData = new MatchData();
        matchData.setId(filesDetails.getId());
        String localName = fullNormalize(filesDetails.getName());

        if (filesDetails.getUserSelectMatchId() != null && filesDetails.getUserSelectMatchId() != -1) {
            ScraperSubject scraperSubject = getOriginalDataByUserSelectMatchId(filesDetails.getUserSelectMatchId());
            if (scraperSubject != null) {
                ScoreData scoreData = new ScoreData();
                scoreData.setData(scraperSubject);
                scoreData.setScore(1);
                matchData.setType(true);
                matchData.setScraperSubject(scoreData.getData());
                return matchData;
            }
        }

        String localOriginalName = fullNormalize(filesDetails.getOriginalName());
        List<ScraperSubject> originalDataList = getOriginalDataList(filesDetails.getName());
        List<ScoreData> scoreList = new ArrayList<>();
        double score = 0.0;
        for (ScraperSubject originalData : originalDataList) {
            score = handleScore(localName, localOriginalName, originalData);
            if (score > 0) {
                ScoreData scoreData = new ScoreData();
                scoreData.setData(originalData);
                scoreData.setScore(score);
                scoreList.add(scoreData);
            }
        }
        if (scoreList.isEmpty()) {
            matchData.setType(false);
            return matchData;
        }
        scoreList.sort((a, b) -> Double.compare(b.score, a.score));
        ScoreData scoreData = scoreList.get(0);
        if (scoreData.getScore() < PASS_SCORE) {
            matchData.setType(false);
            return matchData;
        }
        matchData.setType(true);
        matchData.setScraperSubject(scoreData.getData());
        return matchData;
    }

    private String fullNormalize(String input) {
        if (input == null || input.isBlank()) return "";
        String s = input;
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.replaceAll("[（\\(【\\[「].*?[）\\)】\\]」]", "");
        s = s.replaceAll("[\\p{Punct}]+", " ");
        s = s.replaceAll("\\s+", " ").trim().toLowerCase();
        s = s.replaceAll("(?i)\\b(第?[0-9０-９]+[卷话集期章部]?|vol\\.?\\s*\\d+|v\\d+)\\b", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private double handleScore(String localName, String localOriginalName, ScraperSubject originalData) {
        String bgmName = fullNormalize(originalData.getName());
        String bgmNameCN = fullNormalize(originalData.getNameCn());
        String bgmAlias = fullNormalize(originalData.getAlias());
        double score = 0.0;

        if ((!bgmNameCN.isBlank() && (localName.equals(bgmNameCN) || localOriginalName.equals(bgmNameCN))) ||
                localName.equals(bgmName) || localOriginalName.equals(bgmName) ||
                localName.equals(bgmAlias) || localOriginalName.equals(bgmAlias)) {
            return 1.0;
        }

        Set<String> localTokens = tokenize(localName);
        Set<String> localOriginalTokens = tokenize(localOriginalName);
        Set<String> bgmNameTokens = tokenize(bgmName);
        Set<String> bgmNameCNTokens = tokenize(bgmNameCN);
        Set<String> bgmAliasTokens = tokenize(bgmAlias);

        double tokenScore = jaccard(localTokens, bgmNameTokens);
        tokenScore = Math.max(tokenScore, jaccard(localOriginalTokens, bgmNameTokens));
        tokenScore = Math.max(tokenScore, jaccard(localTokens, bgmNameCNTokens));
        tokenScore = Math.max(tokenScore, jaccard(localOriginalTokens, bgmNameCNTokens));
        tokenScore = Math.max(tokenScore, jaccard(localTokens, bgmAliasTokens));
        tokenScore = Math.max(tokenScore, jaccard(localOriginalTokens, bgmAliasTokens));

        if (tokenScore * 0.92 > score) score = tokenScore * 0.92;

        double sim = similarity(localName, bgmNameCN);
        sim = Math.max(sim, similarity(localName, bgmName));
        sim = Math.max(sim, similarity(localOriginalName, bgmName));
        sim = Math.max(sim, similarity(localOriginalName, bgmNameCN));
        sim = Math.max(sim, similarity(localName, bgmAlias));
        sim = Math.max(sim, similarity(localOriginalName, bgmAlias));

        if (sim * 0.90 > score) score = sim * 0.90;

        for (String conflict : CONFLICT_WORDS) {
            boolean folderHas = localName.contains(conflict) || localOriginalName.contains(conflict);
            boolean seriesHas = bgmName.contains(conflict) || bgmNameCN.contains(conflict) || bgmAlias.contains(conflict);
            if (folderHas != seriesHas) {
                score *= 0.75;
                break;
            }
        }
        return score;
    }


    private Set<String> tokenize(String name) {
        return Arrays.stream(name.split("\\s+")).filter(t -> t.length() > 1).collect(Collectors.toSet());
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        HashSet<String> i = new HashSet<>(a);
        i.retainAll(b);
        HashSet<String> u = new HashSet<>(a);
        u.addAll(b);
        return u.isEmpty() ? 0.0 : (double) i.size() / u.size();
    }

    private double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - (double) levenshtein(a, b) / maxLen;
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    public void init() {
        try {
            redisTemplate.delete(List.of(TAG_FILES, AUTHOR_FILES, TAGS, AUTHORS));
            Map<String, String> redisMap = new HashMap<>();
            filesTagsMapper.selectList(null)
                    .stream()
                    .collect(Collectors.groupingBy(FilesTags::getFilesId, Collectors.mapping(FilesTags::getTagsId, Collectors.toList())))
                    .forEach((filesId, tagIds) -> {
                        try {
                            redisMap.put(String.valueOf(filesId), objectMapper.writeValueAsString(tagIds));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });

            if (!redisMap.isEmpty()) {
                redisTemplate.opsForHash().putAll(TAG_FILES, redisMap);
                redisMap.clear();
            }

            filesAuthorMapper.selectList(null)
                    .stream()
                    .collect(Collectors.groupingBy(FilesAuthor::getFilesId,
                            Collectors.mapping(FilesAuthor::getAuthorId, Collectors.toList())
                    ))
                    .forEach((filesId, authorIds) -> {
                        try {
                            redisMap.put(String.valueOf(filesId), objectMapper.writeValueAsString(authorIds));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });

            if (!redisMap.isEmpty()) {
                redisTemplate.opsForHash().putAll(AUTHOR_FILES, redisMap);
                redisMap.clear();
            }

            tagsMapper.selectList(null).forEach(tag -> redisMap.put(tag.getName(), String.valueOf(tag.getId())));
            if (!redisMap.isEmpty()) {
                redisTemplate.opsForHash().putAll(TAGS, redisMap);
                redisMap.clear();
            }

            authorMapper.selectList(null)
                    .forEach(author -> redisMap.put(author.getName(), String.valueOf(author.getId())));
            if (!redisMap.isEmpty()) redisTemplate.opsForHash().putAll(AUTHORS, redisMap);
            init = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化刮削缓存失败", e);
        }
    }

    public abstract String getName();

    public abstract String getAuthorAvatarPath(Integer authorId);

    private void bindTagsAndPerson(Map<String, List<Long>> missingTags, Map<String,ScraperPerson> missingAuthors, Map<String,List<Long>> missingAuthorsByFileIds,List<FilesAuthor> authorsId, List<FilesTags> tagsId) {
        if (!missingTags.isEmpty()) {
            List<Tags> tagsList = tagsService.createTags(
                    missingTags.keySet().stream()
                            .map(name -> {
                                Tags tags = new Tags();
                                tags.setName(name);
                                return tags;
                            }).toList()
            );

            for (Tags tags : tagsList) {
                List<Long> fileIds = missingTags.get(tags.getName());
                for (Long id : fileIds) {
                    FilesTags filesTags = new FilesTags();
                    filesTags.setFilesId(id);
                    filesTags.setTagsId(tags.getId());
                    tagsId.add(filesTags);
                }
            }
            missingTags.clear();
        }

        if (!missingAuthors.isEmpty()) {
            List<Author> authorsList = authorService.createAuthors(
                    missingAuthors.values()
                            .stream()
                            .map(item -> {
                                Author author = new Author();
                                author.setName(item.getName());
                                author.setBgmId(item.getId());
                                author.setProfile(item.getSummary());
                                StringBuilder careers = new StringBuilder();
                                for (String career : item.getCareer()) {
                                    careers.append(CAREER.get(career));
                                }
                                author.setAvatar(getAuthorAvatarPath(author.getBgmId()));
                                try {
                                    log.info("请求获取制作人员图片，休息5秒");
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                author.setVocational(careers.toString());
                                return author;
                            })
                            .toList()
            );



            for (Author author : authorsList) {
                List<Long> fileIds = missingAuthorsByFileIds.get(author.getName());
                for (Long id : fileIds) {
                    FilesAuthor filesAuthor = new FilesAuthor();
                    filesAuthor.setFilesId(id);
                    filesAuthor.setAuthorId(author.getId());
                    authorsId.add(filesAuthor);
                }
            }

            missingAuthors.clear();
        }

        if (!tagsId.isEmpty()) {
            filesTagsService.createList(tagsId);
            tagsId.clear();
        }

        if (!authorsId.isEmpty()) {
            filesAuthorService.createList(authorsId);
            authorsId.clear();
        }
    }
}

@Data
class ScoreData {
    ScraperSubject data;
    double score;
}

@Data
class MatchData {
    Integer id;
    ScraperSubject scraperSubject;
    Boolean type;
}