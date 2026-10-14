package io.github.qqaazz2.DReaderServer.handle;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperPerson;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperTag;

import java.util.List;

public class ScraperPersonListHandler extends JsonListTypeHandler<ScraperPerson> {
    public ScraperPersonListHandler() {
        super(new TypeReference<List<ScraperPerson>>() {});
    }
}