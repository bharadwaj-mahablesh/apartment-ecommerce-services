# Apartment Community Marketplace

This project is an Apartment Community Marketplace, built as a microservices-based application using Spring Boot and Maven.

## Current Status

We have established a multi-module Maven project structure, including the following core services:

*   `apartment-management-service`: Manages apartment listings and related data.
*   `user-management-service`: An IAM service responsible for user authentication (JWT) and authorization, with pre-populated roles (PENDING_USER, RESIDENT, ADMIN).
*   `notification-service`: Handles notifications, primarily email-based.
*   `api-gateway-service`: Acts as the entry point for all client requests, routing them to the appropriate microservices.
*   `eureka-server`: Provides service discovery capabilities, allowing microservices to register and discover each other.
*   `common-events`: A shared module for defining common event structures used across microservices (e.g., `UserRegisteredEvent`, `UserStatusChangedEvent`).

All core microservices are integrated with Eureka for service discovery and are accessible via the API Gateway. Kafka connection and security issues in `user-management-service` tests have been resolved, and event classes have been refactored into the `common-events` module.

The project has been initialized in a Git repository, with `.gitignore` configured to exclude unnecessary files like IDE metadata and build artifacts.

## Next Objectives

Our immediate next steps are:

1.  **Address `NotificationServiceIT` Tests:** Investigate and ensure the `NotificationServiceIT` tests are passing and fully functional.
2.  **Configure Email Authentication:** Implement and configure the necessary email authentication details for the `notification-service` to enable actual email sending.

## Getting Started

(Instructions on how to set up and run the project will be added here as the project progresses.)
