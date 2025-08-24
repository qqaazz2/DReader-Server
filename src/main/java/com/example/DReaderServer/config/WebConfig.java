package com.example.DReaderServer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")  // 适用于所有API端点
                        .allowedOrigins("http://localhost:62182")  // 允许的源
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的方法
                        .allowedHeaders("*")  // 允许的头
                        .allowCredentials(true);  // 允许发送Cookie信息
            }
        };
    }
}
