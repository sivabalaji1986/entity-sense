package com.hbs.entitysense.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static com.hbs.entitysense.constants.EntitySenseConstant.OLLAMA_URL;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient ollamaClient() {
        return WebClient.builder()
                .baseUrl(OLLAMA_URL)
                .build();
    }
}
