package io.github.qqaazz2.DReaderServer.entity.scraper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.qqaazz2.DReaderServer.entity.Tags;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@TableName(value = "bgm_subject", autoResultMap = true)
@Data
public class ScraperSubject {
    Integer id;
    String name;
    String nameCn;
    String alias;
    Integer type;
    String summary;
    String date;

    @TableField(typeHandler = JacksonTypeHandler.class)
    List<ScraperPerson> persons = new ArrayList<>();

    @TableField(typeHandler = JacksonTypeHandler.class)
    List<ScraperTag> tags = new ArrayList<>();
}