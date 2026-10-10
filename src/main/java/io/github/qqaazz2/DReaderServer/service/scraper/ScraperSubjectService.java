package io.github.qqaazz2.DReaderServer.service.scraper;

import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperSubject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ScraperSubjectService {
    List<ScraperSubject> createDataAll(List<ScraperSubject> list);
}
