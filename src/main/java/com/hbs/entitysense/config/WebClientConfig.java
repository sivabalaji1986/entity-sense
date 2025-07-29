package com.hbs.entitysense.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient ollamaClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }
}
