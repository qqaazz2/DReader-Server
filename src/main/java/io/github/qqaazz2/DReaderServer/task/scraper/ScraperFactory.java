package io.github.qqaazz2.DReaderServer.task.scraper;

import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.entity.LoginUser;
import io.github.qqaazz2.DReaderServer.enums.ExceptionEnum;
import io.github.qqaazz2.DReaderServer.storage.FileAdapterService;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScraperFactory {
    private Map<String, BaseScraper> scraperMap = new HashMap<>();

    public ScraperFactory(List<BaseScraper> list) {
        for (BaseScraper baseScraper : list) {
            scraperMap.put(baseScraper.getName(), baseScraper);
        }
    }

    public BaseScraper getScraper(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR);
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String type = loginUser.getUser().getScraper();
        BaseScraper scraper = scraperMap.get(type);
        if (scraper == null) throw new BizException("4000", "未找到对应的刮削器");
        return scraper;
    }
}
