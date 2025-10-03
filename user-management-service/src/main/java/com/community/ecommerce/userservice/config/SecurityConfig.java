package com.community.ecommerce.userservice.config;

import com.community.ecommerce.userservice.security.CustomUserDetailsService;
import com.community.ecommerce.userservice.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/v1/users/register", "/api/v1/auth/login").permitAll() // Allow registration and login without authentication
                .requestMatchers("/api/v1/users").hasRole("ADMIN") // Only ADMIN can list all users
                .requestMatchers("/api/v1/users/{id}").access(new WebExpressionAuthorizationManager("hasRole('ADMIN') or (hasRole('RESIDENT') and #id == authentication.principal.id)")) // ADMIN can get any user, RESIDENT can get their own
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/{id}").access(new WebExpressionAuthorizationManager("hasRole('ADMIN') or (hasRole('RESIDENT') and #id == authentication.principal.id)")) // ADMIN can update any user, RESIDENT can update their own
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasRole("ADMIN") // Only ADMIN can delete users
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/{id}/password").access(new WebExpressionAuthorizationManager("hasRole('ADMIN') or (hasRole('RESIDENT') and #id == authentication.principal.id)")) // ADMIN can change any user's password, RESIDENT can change their own
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/{id}/status").hasRole("ADMIN") // Only ADMIN can approve/reject users
                .anyRequest().authenticated() // All other requests require authentication
            )
            .httpBasic(withDefaults()); // Enable HTTP Basic Authentication

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
