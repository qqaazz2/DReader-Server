package io.github.qqaazz2.DReaderServer.dto.readLog;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReadLogStatisticsDTO {
    String date;
    Integer minutes;
}
