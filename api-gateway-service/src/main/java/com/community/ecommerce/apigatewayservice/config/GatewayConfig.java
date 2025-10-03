package com.community.ecommerce.apigatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("usermanagementservice", r -> r.path("/api/v1/users/**", "/api/v1/auth/**")
                        .uri("lb://usermanagementservice"))
                .route("apartmentservice", r -> r.path("/api/v1/apartments/**")
                        .uri("lb://apartmentservice"))
                .route("notificationservice", r -> r.path("/api/v1/notifications/**")
                        .uri("lb://notificationservice"))
                .build();
    }
}
