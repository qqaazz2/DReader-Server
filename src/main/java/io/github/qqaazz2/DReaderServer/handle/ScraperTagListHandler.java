package io.github.qqaazz2.DReaderServer.handle;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperTag;

import java.util.List;

public class ScraperTagListHandler
        extends JsonListTypeHandler<ScraperTag> {

    public ScraperTagListHandler() {
        super(new TypeReference<List<ScraperTag>>() {});
    }
}