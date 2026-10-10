package io.github.qqaazz2.DReaderServer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${webclient.proxy.host}")
    private String proxyHost;

    @Value("${webclient.proxy.port}")
    private int proxyPort;

    private HttpClient httpClient() {
        HttpClient client = HttpClient.create().followRedirect(true);
        if (proxyHost != null && !proxyHost.isBlank()) {
            client = client.proxy(proxy -> proxy
                    .type(ProxyProvider.Proxy.HTTP)
                    .host(proxyHost)
                    .port(proxyPort));
        }
        return client;
    }

    private WebClient.Builder builder() {
        return WebClient.builder()
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

    @Bean
    public WebClient bgmWebClient() {
        return builder()
                .baseUrl("https://api.bgm.tv")
                .build();
    }

    @Bean
    public WebClient githubWebClient() {
        return builder().build();
    }
}