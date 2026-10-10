package io.github.qqaazz2.DReaderServer.dto.files;

import lombok.Data;

@Data
public class FilesOverviewDTO {
    public Integer seriesCount = 0;
    public Integer bookCount = 0;
    public Integer overCount = 0;
    public Integer unreadCount = 0;
    public Integer readingCount = 0;
    public Integer loveSeriesCount = 0;
    public Integer loveBookCount = 0;
    public Integer overSeriesCount = 0;
    public Integer unOverSeriesCount = 0;
    public Integer discardedSeriesCount = 0;
}
