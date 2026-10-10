package io.github.qqaazz2.DReaderServer.dto.readLog;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReadLogDTO {
    protected Integer id;
    protected Integer minutes;
    protected Integer book_id;
    protected String book_name;
    protected String time;
}
