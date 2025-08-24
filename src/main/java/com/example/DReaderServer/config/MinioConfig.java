package com.example.DReaderServer.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Value("${minio.url}")
    String url;

    @Value("${minio.access}")
    String access;

    @Value("${minio.secret}")
    String secret;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder().endpoint(url).credentials(access,secret).build();
    }
}
