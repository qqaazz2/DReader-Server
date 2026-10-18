package io.github.qqaazz2.DReaderServer.config;

import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.entity.LoginUser;
import io.github.qqaazz2.DReaderServer.entity.User;
import io.github.qqaazz2.DReaderServer.enums.ExceptionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Slf4j
@Component
public class WebClientConfig {
    WebClient.Builder builder;
    boolean initOver = false;

    public WebClientConfig(){
        builder();
    }

    private HttpClient httpClient() {
        HttpClient client = HttpClient.create().followRedirect(true);
        if(initOver){
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
                LoginUser loginUser = (LoginUser) authentication.getPrincipal();
                User user = loginUser.getUser();
                if(user.getProxyEnabled() == 2 || (user.getProxyEnabled() == 1 && (user.getProxyHost() == null || user.getProxyPort() == null))) return client;
                client = client.proxy(proxy -> proxy
                        .type(ProxyProvider.Proxy.HTTP)
                        .host(user.getProxyHost())
                        .port(user.getProxyPort()));
            } else {
                throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR);
            }
        }
        return client;
    }

    private void builder() {
        builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                    log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
                    log.info("Request Headers: {}", clientRequest.headers());
                    log.info("Request Body: {}", clientRequest.body());
                    return Mono.just(clientRequest);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
                    log.info("Response Status: {}", clientResponse.statusCode());
                    return Mono.just(clientResponse);
                }))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                        .build());
    }

    public void resetInitOver(boolean initOver) {
        this.initOver = initOver;
        builder();
    }

    public WebClient bgmWebClient() {
        return builder
                .baseUrl("https://api.bgm.tv")
                .build();
    }

    public WebClient githubWebClient() {
        return builder.build();
    }
}