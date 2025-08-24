package com.example.DReaderServer.controller;


import com.example.DReaderServer.channel.ScanningSseClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/sse")
public class SseController {
    @Resource
    ScanningSseClient sseClient;

    @GetMapping("/createSse")
    public SseEmitter createSse(){
        log.info("asdasdasd");
        SseEmitter sseEmitter = sseClient.createSse();
        sseClient.sendMessage();
        return sseEmitter;
    }

    @GetMapping("/closeSse")
    public void closeConnect(){
        sseClient.closeSse();
    }
}
