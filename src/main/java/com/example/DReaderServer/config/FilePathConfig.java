package com.example.DReaderServer.config;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class FilePathConfig implements WebMvcConfigurer {
    @Value("${file.upload}")
    String url;

    @PostConstruct
    public void init() {
        File directory = new File(url);
        if (!directory.exists()) directory.mkdirs();
    }

    @SneakyThrows
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File file = new File(url);
        registry.addResourceHandler("/files/**").addResourceLocations("file:" + file.getAbsolutePath() + "/");
    }
}
