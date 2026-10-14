package io.github.qqaazz2.DReaderServer.entity.scraper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScraperPerson {
    String name;
    Integer id;
    Integer type;
    List<String> career;
    String summary;
}
