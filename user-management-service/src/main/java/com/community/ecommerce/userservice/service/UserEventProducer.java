package com.community.ecommerce.userservice.service;

import com.community.ecommerce.common.events.UserRegisteredEvent;
import com.community.ecommerce.common.events.UserStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(UserEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        logger.info("Publishing UserRegisteredEvent: {}", event);
        kafkaTemplate.send("user-registered-events", event.userId().toString(), event);
    }

    public void publishUserStatusChangedEvent(UserStatusChangedEvent event) {
        logger.info("Publishing UserStatusChangedEvent: {}", event);
        kafkaTemplate.send("user-status-changed-events", event.userId().toString(), event);
    }
}
