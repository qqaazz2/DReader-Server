package io.github.qqaazz2.DReaderServer.service.impl.scraper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperSubject;
import io.github.qqaazz2.DReaderServer.mapper.bgm.ScraperSubjectMapper;
import io.github.qqaazz2.DReaderServer.service.scraper.ScraperSubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ScraperSubjectServiceImpl extends ServiceImpl<ScraperSubjectMapper, ScraperSubject> implements ScraperSubjectService {

    @Override
    @Transactional
    public List<ScraperSubject> createDataAll(List<ScraperSubject> list) {
        if(!this.saveBatch(list,1000)){
            throw new BizException("4000", "新增BgmArchive数据失败");
        }
        return list;
    }
}
