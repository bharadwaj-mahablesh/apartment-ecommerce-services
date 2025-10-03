package com.community.ecommerce.notificationservice.listener;

import com.community.ecommerce.common.events.UserRegisteredEvent;
import com.community.ecommerce.common.events.UserStatusChangedEvent;
import com.community.ecommerce.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);

    private final EmailService emailService;

    @KafkaListener(topics = "user-registered-events", groupId = "notification-group")
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        logger.info("Received UserRegisteredEvent: {}", event);
        String subject = "New User Registration for Apartment Approval";
        String text = String.format(
                "A new user has registered for apartment %d, block %s, apartment number %s.\n" +
                "User Details: %s %s (%s).\n" +
                "Please review and approve/reject their registration.",
                event.apartmentId(), event.blockName(), event.apartmentNumber(),
                event.firstName(), event.lastName(), event.email()
        );
        // In a real scenario, you would fetch apartment contact emails here
        // For now, sending to a placeholder email or logging
        emailService.sendSimpleMessage("apartment.contact@example.com", subject, text);
    }

    @KafkaListener(topics = "user-status-changed-events", groupId = "notification-group")
    public void handleUserStatusChangedEvent(UserStatusChangedEvent event) {
        logger.info("Received UserStatusChangedEvent: {}", event);
        String subject = String.format("Your Registration Status for Apartment %d has Changed", event.apartmentId());
        String text = String.format(
                "Dear %s %s,\n\nYour registration for apartment %d, block %s, apartment number %s has been updated.\n" +
                "New Status: %s.\n" +
                "New Role: %s.\n\nThank you.",
                event.firstName(), event.lastName(), event.apartmentId(), event.blockName(), event.apartmentNumber(),
                event.newStatus(), event.newRoleName()
        );
        emailService.sendSimpleMessage(event.email(), subject, text);
    }
}
