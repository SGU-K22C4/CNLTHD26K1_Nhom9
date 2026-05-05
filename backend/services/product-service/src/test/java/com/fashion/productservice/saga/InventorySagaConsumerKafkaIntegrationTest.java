package com.fashion.productservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.common.event.InventoryReservationResultEvent;
import com.fashion.common.event.OrderCreatedEvent;
import com.fashion.common.event.OrderItemEvent;
import com.fashion.common.event.SagaTopics;
import com.fashion.productservice.entity.VariantSize;
import com.fashion.productservice.repository.saga.VariantSizeSagaRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = InventorySagaConsumerKafkaIntegrationTest.TestApplication.class,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
        }
)
@EmbeddedKafka(partitions = 1, topics = {SagaTopics.ORDER_CREATED, SagaTopics.INVENTORY_RESERVATION_RESULT})
class InventorySagaConsumerKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VariantSizeSagaRepository variantSizeSagaRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("product-consumer-test", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, SagaTopics.INVENTORY_RESERVATION_RESULT);
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close(Duration.ofSeconds(1));
        }
    }

    @Test
    void should_ConsumeOrderCreatedAndPublishReservationSuccess_When_InventoryIsAvailable() throws Exception {
        VariantSize variantSize = VariantSize.builder()
                .id("VS-1")
                .quantity(10)
                .status("Con hang")
                .build();
        when(variantSizeSagaRepository.findForUpdate("PROD-1", "Black", "M")).thenReturn(List.of(variantSize));

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(88L)
                .items(List.of(OrderItemEvent.builder()
                        .productId("PROD-1")
                        .color("Black")
                        .size("M")
                        .quantity(2)
                        .build()))
                .build();

        kafkaTemplate.send(SagaTopics.ORDER_CREATED, "88", objectMapper.writeValueAsString(event));

        ConsumerRecord<String, String> record = waitForRecord("88");
        InventoryReservationResultEvent payload =
                objectMapper.readValue(record.value(), InventoryReservationResultEvent.class);

        verify(variantSizeSagaRepository, timeout(10000)).saveAll(ArgumentMatchers.anyList());
        assertEquals(8, variantSize.getQuantity());
        assertEquals("88", record.key());
        assertTrue(payload.isSuccess());
        assertEquals(88L, payload.getOrderId());
    }

    @Test
    void should_PublishReservationFailure_When_VariantSizeIsMissing() throws Exception {
        when(variantSizeSagaRepository.findForUpdateWithoutColor("PROD-2", "L")).thenReturn(List.of());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(99L)
                .items(List.of(OrderItemEvent.builder()
                        .productId("PROD-2")
                        .size("L")
                        .quantity(1)
                        .build()))
                .build();

        kafkaTemplate.send(SagaTopics.ORDER_CREATED, "99", objectMapper.writeValueAsString(event));

        ConsumerRecord<String, String> record = waitForRecord("99");
        InventoryReservationResultEvent payload =
                objectMapper.readValue(record.value(), InventoryReservationResultEvent.class);

        assertFalse(payload.isSuccess());
        assertTrue(payload.getReason().contains("Variant size not found"));
    }

    private ConsumerRecord<String, String> waitForRecord(String key) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1));
            for (ConsumerRecord<String, String> record : records.records(SagaTopics.INVENTORY_RESERVATION_RESULT)) {
                if (key.equals(record.key())) {
                    return record;
                }
            }
        }
        throw new IllegalStateException("Did not receive Kafka record for key=" + key);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EnableKafka
    @EnableKafkaRetryTopic
    static class TestApplication {

        @Bean
        TaskScheduler taskScheduler() {
            return new ThreadPoolTaskScheduler();
        }

        @Bean
        SagaEventPublisher sagaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
            return new SagaEventPublisher(kafkaTemplate, objectMapper);
        }

        @Bean
        InventorySagaConsumer inventorySagaConsumer(
                ObjectMapper objectMapper,
                VariantSizeSagaRepository variantSizeSagaRepository,
                SagaEventPublisher sagaEventPublisher
        ) {
            return new InventorySagaConsumer(objectMapper, variantSizeSagaRepository, sagaEventPublisher);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new PlatformTransactionManager() {
                @Override
                public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                    return new SimpleTransactionStatus();
                }

                @Override
                public void commit(TransactionStatus status) throws TransactionException {
                }

                @Override
                public void rollback(TransactionStatus status) throws TransactionException {
                }
            };
        }
    }
}
