package io.github.qqaazz2.DReaderServer.service;

import io.github.qqaazz2.DReaderServer.dto.logs.LogDTO;
import io.github.qqaazz2.DReaderServer.dto.PageVO;
import io.github.qqaazz2.DReaderServer.dto.logs.LogListQueryCondition;
import org.springframework.stereotype.Service;

@Service
public interface LoggingEventService {
    PageVO<LogDTO> getLogList(LogListQueryCondition condition);

}
