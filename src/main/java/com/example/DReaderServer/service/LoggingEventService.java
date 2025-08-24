package com.example.DReaderServer.service;

import com.example.DReaderServer.dto.logs.LogDTO;
import com.example.DReaderServer.dto.PageVO;
import com.example.DReaderServer.dto.QueryCondition;
import com.example.DReaderServer.dto.logs.LogListQueryCondition;
import org.springframework.stereotype.Service;

@Service
public interface LoggingEventService {
    PageVO<LogDTO> getLogList(LogListQueryCondition condition);

}
