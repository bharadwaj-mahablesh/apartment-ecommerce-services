package com.community.ecommerce.notificationservice;

import com.community.ecommerce.common.events.UserRegisteredEvent;
import com.community.ecommerce.common.events.UserStatus;
import com.community.ecommerce.common.events.UserStatusChangedEvent;
import com.community.ecommerce.notificationservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@EnableKafka
public class NotificationServiceIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.0"));

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);

        // Consumer deserialization properties for tests
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
    }

    @Configuration
    static class KafkaTestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public ProducerFactory<String, Object> producerFactory() {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put(BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            configProps.put(KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
            configProps.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            // Configure JsonSerializer to not add type headers for simpler JSON
            configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
            return new DefaultKafkaProducerFactory<>(configProps);
        }

        @Bean
        public KafkaTemplate<String, Object> testKafkaTemplate(ProducerFactory<String, Object> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            return new DefaultKafkaConsumerFactory<>(
                    props,
                    new StringDeserializer(),
                    new JsonDeserializer<>(objectMapper).trustedPackages("*")
            );
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
                ConsumerFactory<String, Object> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            return factory;
        }
    }

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        //Mockito.reset(emailService);
    }

    @AfterEach
    void tearDown() {
        // Clean up any test-specific data if necessary
    }

    @Test
    void testUserRegisteredEventConsumption() throws Exception {
        UserRegisteredEvent event = new UserRegisteredEvent(
                1L, "test@example.com", "Test", "User", 101L, "Block A", "101", null, "PENDING_USER");

        kafkaTemplate.send("user-registered-events", event.userId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            String expectedSubject = "New User Registration for Apartment Approval";
            String expectedText = String.format(
                    "A new user has registered for apartment %d, block %s, apartment number %s.\n" +
                    "User Details: %s %s (%s).\n" +
                    "Please review and approve/reject their registration.",
                    event.apartmentId(), event.blockName(), event.apartmentNumber(),
                    event.firstName(), event.lastName(), event.email()
            );
            verify(emailService, timeout(5000).times(1)).sendSimpleMessage(
                    eq("apartment.contact@example.com"), eq(expectedSubject), eq(expectedText));
        });
    }

    @Test
    void testUserStatusChangedEventConsumption() throws Exception {
        UserStatusChangedEvent event = new UserStatusChangedEvent(
                1L, "test@example.com", "Test", "User", UserStatus.PENDING_APPROVAL, UserStatus.APPROVED, "PENDING_USER", "RESIDENT", 101L, "Block A", "101");

        kafkaTemplate.send("user-status-changed-events", event.userId().toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            String expectedSubject = String.format("Your Registration Status for Apartment %d has Changed", event.apartmentId());
            String expectedText = String.format(
                    "Dear %s %s,\n\nYour registration for apartment %d, block %s, apartment number %s has been updated.\n" +
                    "New Status: %s.\n" +
                    "New Role: %s.\n\nThank you.",
                    event.firstName(), event.lastName(), event.apartmentId(), event.blockName(), event.apartmentNumber(),
                    event.newStatus(), event.newRoleName()
            );
            verify(emailService, timeout(5000).times(1)).sendSimpleMessage(
                    eq(event.email()), eq(expectedSubject), eq(expectedText));
        });
    }
}