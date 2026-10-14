package io.github.qqaazz2.DReaderServer.entity.scraper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.qqaazz2.DReaderServer.entity.Tags;
import io.github.qqaazz2.DReaderServer.handle.ScraperPersonListHandler;
import io.github.qqaazz2.DReaderServer.handle.ScraperTagListHandler;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@TableName(value = "bgm_subject", autoResultMap = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ScraperSubject {
    Integer id;
    String name;
    String nameCn;
    String alias;
    Integer type;
    String summary;
    String date;

    @TableField(typeHandler = ScraperPersonListHandler.class)
    List<ScraperPerson> persons = new ArrayList<>();

    @TableField(typeHandler = ScraperTagListHandler.class)
    List<ScraperTag> tags = new ArrayList<>();
}