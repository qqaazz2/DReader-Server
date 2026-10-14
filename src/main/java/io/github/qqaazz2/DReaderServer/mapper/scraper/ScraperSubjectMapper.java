package io.github.qqaazz2.DReaderServer.mapper.scraper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qqaazz2.DReaderServer.entity.scraper.ScraperSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ScraperSubjectMapper extends BaseMapper<ScraperSubject> {
    List<ScraperSubject> search(@Param("keyword") String keyword);

    @Update("""
    DROP TABLE IF EXISTS bgm_subject
    """)
    void dropTable();

    @Update("""
    CREATE TABLE bgm_subject (
        id BIGINT NOT NULL,
        name VARCHAR(500) NOT NULL,
        name_cn VARCHAR(500),
        alias TEXT,
        type TINYINT,
        date VARCHAR(32),
        summary TEXT,
        tags JSON,
        persons JSON,
        PRIMARY KEY (id),
        FULLTEXT KEY ft_bgm_subject_search (name, name_cn, alias)
    ) ENGINE=InnoDB
      DEFAULT CHARSET=utf8mb4
      COLLATE=utf8mb4_unicode_ci
    """)
    void createTable();
}
