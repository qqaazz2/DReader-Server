package io.github.qqaazz2.DReaderServer.entity.scraper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScraperTag {
    String name;
}
