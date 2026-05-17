package com.fashion.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class KafkaRetryTopicConfig {

    @Bean(name = "kafkaRetryTaskScheduler")
    public TaskScheduler kafkaRetryTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("product-kafka-retry-");
        return scheduler;
    }
}