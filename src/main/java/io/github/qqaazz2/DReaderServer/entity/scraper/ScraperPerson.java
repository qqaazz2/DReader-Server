package io.github.qqaazz2.DReaderServer.entity.scraper;

import lombok.Data;

import java.util.List;

@Data
public class ScraperPerson {
    String name;
    Integer id;
    Integer type;
    List<String> career;
    String summary;
}
