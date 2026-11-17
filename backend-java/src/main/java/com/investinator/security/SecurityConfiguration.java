package com.investinator.security;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfiguration {
    @Bean UserDetailsService noInteractiveUsers() {
        return name -> { throw new UsernameNotFoundException("Interactive authentication is not configured"); };
    }
    @Bean SecurityFilterChain security(HttpSecurity http,
            @Value("${investinator.metrics.token:}") String token) throws Exception {
        return http.authorizeHttpRequests(a -> a
            .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness", "/api/v1/platform/health").permitAll()
            .requestMatchers("/actuator/prometheus").access((auth, context) -> {
                String header = context.getRequest().getHeader("Authorization");
                boolean allowed = !token.isBlank() && header != null && MessageDigest.isEqual(
                    ("Bearer " + token).getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8));
                return new AuthorizationDecision(allowed);
            })
            .anyRequest().denyAll()).build();
    }
}