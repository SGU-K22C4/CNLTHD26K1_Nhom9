package com.fashion.orderservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.common.event.SagaTopics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = SagaPublisherKafkaIntegrationTest.TestApplication.class,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
        }
)
@EmbeddedKafka(partitions = 1, topics = {SagaTopics.ORDER_CREATED})
class SagaPublisherKafkaIntegrationTest {

    @Autowired
    private SagaEventPublisher sagaEventPublisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("order-publisher-test", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, SagaTopics.ORDER_CREATED);
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close(Duration.ofSeconds(1));
        }
    }

    @Test
    void should_PublishOrderCreatedEventToKafka_When_PublisherIsInvoked() throws Exception {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(101L)
                .orderNumber("ORD-101")
                .paymentMethod("COD")
                .items(List.of(OrderItemEvent.builder()
                        .productId("PROD-1")
                        .color("Black")
                        .size("M")
                        .quantity(2)
                        .build()))
                .build();

        sagaEventPublisher.publishOrderCreated(event);

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, SagaTopics.ORDER_CREATED, Duration.ofSeconds(10));
        OrderCreatedEvent payload = objectMapper.readValue(record.value(), OrderCreatedEvent.class);

        assertNotNull(record);
        assertEquals("101", record.key());
        assertEquals(101L, payload.getOrderId());
        assertEquals("ORD-101", payload.getOrderNumber());
        assertTrue(payload.getItems().size() == 1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EnableKafka
    static class TestApplication {

        @Bean
        SagaEventPublisher sagaEventPublisher(org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate,
                                              ObjectMapper objectMapper) {
            return new SagaEventPublisher(kafkaTemplate, objectMapper);
        }
    }
}
