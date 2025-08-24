package com.example.DReaderServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;

@Component
public class DReaderServerApplicationRunner implements ApplicationRunner {
    @Value("${file.upload}")
    String filePath;

    String books = "books";
    String[] array = new String[]{books};

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (String path : array) {
            File file = new File(filePath + path);
            if (!file.exists()) file.mkdirs();
        }
    }
}
