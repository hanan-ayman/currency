package com.sanad.currency.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenExchangeRatesConfig {

    @Value("${openexchangerates.base-url}")
    private String baseUrl;
    
    @Value("${openexchangerates.api-key}")
    private String apiKey;
    
    @Bean
    public WebClient openExchangeRatesWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Token " + apiKey)
                .build();
    }
}
