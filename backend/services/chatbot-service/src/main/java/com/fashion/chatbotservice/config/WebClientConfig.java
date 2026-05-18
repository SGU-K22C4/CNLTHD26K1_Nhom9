package com.fashion.chatbotservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${chatbot.internal.caller:chatbot-service}")
    private String internalCaller;

    @Value("${chatbot.internal.shared-secret:local-chatbot-internal-secret}")
    private String internalSharedSecret;

    /**
     * Max buffer size for WebClient responses.
     * Default Spring is 256KB which is too small for product-service
     * returning 200 products in a single page (~400KB+ JSON).
     * Set to 2MB to handle large product catalogue responses.
     */
    private static final int MAX_IN_MEMORY_SIZE = 2 * 1024 * 1024; // 2 MB

    @Bean
    public WebClient webClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();

        return WebClient.builder()
                .exchangeStrategies(strategies)
                // Internal service identity for chatbot -> downstream microservice calls.
                // This complements gateway identity instead of trusting bare internal traffic.
                .defaultHeader("X-Internal-Caller", internalCaller)
                .defaultHeader("X-Internal-Auth", internalSharedSecret)
                .build();
    }
}
