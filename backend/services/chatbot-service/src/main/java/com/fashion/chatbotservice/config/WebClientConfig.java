package com.fashion.chatbotservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${chatbot.internal.caller:chatbot-service}")
    private String internalCaller;

    @Value("${chatbot.internal.shared-secret:local-chatbot-internal-secret}")
    private String internalSharedSecret;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                // Internal service identity for chatbot -> downstream microservice calls.
                // This complements gateway identity instead of trusting bare internal traffic.
                .defaultHeader("X-Internal-Caller", internalCaller)
                .defaultHeader("X-Internal-Auth", internalSharedSecret)
                .build();
    }
}
