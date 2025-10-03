package com.community.ecommerce.notificationservice.listener;

import com.community.ecommerce.common.events.UserRegisteredEvent;
import com.community.ecommerce.common.events.UserStatusChangedEvent;
import com.community.ecommerce.notificationservice.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserEventListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserEventListener userEventListener;

    private UserRegisteredEvent userRegisteredEvent;
    private UserStatusChangedEvent userStatusChangedEvent;

    @BeforeEach
    void setUp() {
        userRegisteredEvent = new UserRegisteredEvent(
                1L, "john.doe@example.com", "John", "Doe", 101L, "Block A", "101", null, "PENDING_USER");

        userStatusChangedEvent = new UserStatusChangedEvent(
                1L, "john.doe@example.com", "John", "Doe", null, null, null, null, 101L, "Block A", "101");
    }

    @Test
    void handleUserRegisteredEvent_sendsEmail() {
        userEventListener.handleUserRegisteredEvent(userRegisteredEvent);

        String expectedSubject = "New User Registration for Apartment Approval";
        String expectedText = String.format(
                "A new user has registered for apartment %d, block %s, apartment number %s.\n" +
                "User Details: %s %s (%s).\n" +
                "Please review and approve/reject their registration.",
                userRegisteredEvent.apartmentId(), userRegisteredEvent.blockName(), userRegisteredEvent.apartmentNumber(),
                userRegisteredEvent.firstName(), userRegisteredEvent.lastName(), userRegisteredEvent.email()
        );

        verify(emailService, times(1)).sendSimpleMessage(
                eq("apartment.contact@example.com"), eq(expectedSubject), eq(expectedText));
    }

    @Test
    void handleUserStatusChangedEvent_sendsEmail() {
        userEventListener.handleUserStatusChangedEvent(userStatusChangedEvent);

        String expectedSubject = String.format("Your Registration Status for Apartment %d has Changed", userStatusChangedEvent.apartmentId());
        String expectedText = String.format(
                "Dear %s %s,\n\nYour registration for apartment %d, block %s, apartment number %s has been updated.\n" +
                "New Status: %s.\n" +
                "New Role: %s.\n\nThank you.",
                userStatusChangedEvent.firstName(), userStatusChangedEvent.lastName(), userStatusChangedEvent.apartmentId(), userStatusChangedEvent.blockName(), userStatusChangedEvent.apartmentNumber(),
                userStatusChangedEvent.newStatus(), userStatusChangedEvent.newRoleName()
        );

        verify(emailService, times(1)).sendSimpleMessage(
                eq(userStatusChangedEvent.email()), eq(expectedSubject), eq(expectedText));
    }
}
