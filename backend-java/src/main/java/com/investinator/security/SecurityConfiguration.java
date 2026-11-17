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
            @Value("${investinator.metrics.token:}") String token,
            @Value("${investinator.evidence.token:}") String evidenceToken,
            @Value("${investinator.health.token:}") String healthToken,
            @Value("${CONNECTOR_API_TOKEN:}") String connectorToken,
            @Value("${INVESTIGATION_API_TOKEN:}") String investigationToken,
            @Value("${GRAPH_ADMIN_TOKEN:}") String graphAdminToken,
            @Value("${investinator.persistence.enabled:false}") boolean persistence,
            @Value("${investinator.demo-mode:true}") boolean demo) throws Exception {
        return http.sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .csrf(csrf -> { if(persistence && demo) csrf.ignoringRequestMatchers("/api/v1/**", "/incidents/**"); })
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,exception) -> reject(request,response,persistence&&!demo))
                .accessDeniedHandler((request,response,exception) -> reject(request,response,persistence&&!demo)))
            .authorizeHttpRequests(a -> a
            .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness", "/api/v1/platform/health").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/dependencies/fixtures").access((auth,context)->new AuthorizationDecision(demo&&matches(graphAdminToken,context.getRequest().getHeader("Authorization"))))
            .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/dependencies/**").access((auth,context)->new AuthorizationDecision(demo&&matches(investigationToken,context.getRequest().getHeader("Authorization"))))
            .requestMatchers("/api/v1/dependencies/**").denyAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/connectors","/api/v1/connectors/**").access((auth,context) -> {
                String header=context.getRequest().getHeader("Authorization");
                return new AuthorizationDecision(!connectorToken.isBlank()&&header!=null&&demo&&MessageDigest.isEqual(
                    ("Bearer "+connectorToken).getBytes(StandardCharsets.UTF_8),header.getBytes(StandardCharsets.UTF_8)));
            })
            .requestMatchers("/api/v1/connectors","/api/v1/connectors/**").denyAll()
            .requestMatchers("/api/v1/health/integrations").access((auth,context) -> {
                String header=context.getRequest().getHeader("Authorization");
                return new AuthorizationDecision(!healthToken.isBlank()&&header!=null&&demo&&MessageDigest.isEqual(
                    ("Bearer "+healthToken).getBytes(StandardCharsets.UTF_8),header.getBytes(StandardCharsets.UTF_8)));
            })
            .requestMatchers("/api/v1/evidence/**").access((auth,context) -> {
                String header=context.getRequest().getHeader("Authorization");
                return new AuthorizationDecision(!evidenceToken.isBlank()&&header!=null&&demo&&MessageDigest.isEqual(
                    ("Bearer "+evidenceToken).getBytes(StandardCharsets.UTF_8),header.getBytes(StandardCharsets.UTF_8)));
            })
            .requestMatchers("/actuator/prometheus").access((auth, context) -> {
                String header = context.getRequest().getHeader("Authorization");
                boolean allowed = !token.isBlank() && header != null && MessageDigest.isEqual(
                    ("Bearer " + token).getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8));
                return new AuthorizationDecision(allowed);
            })
            .requestMatchers("/health", "/api/v1/**", "/incidents", "/incidents/**", "/command-center", "/pulse", "/nerve-map", "/ai/health", "/audit", "/metrics")
                .access((auth,context) -> new AuthorizationDecision(persistence && demo))
            .anyRequest().denyAll()).build();
    }
    private static boolean matches(String token,String header){return !token.isBlank()&&header!=null&&MessageDigest.isEqual(("Bearer "+token).getBytes(StandardCharsets.UTF_8),header.getBytes(StandardCharsets.UTF_8));}
    private static void reject(jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,boolean nonDemo) throws java.io.IOException {
        boolean missingIdentity=nonDemo&&!request.getRequestURI().startsWith("/actuator/");
        response.setStatus(missingIdentity?503:403);response.setContentType("application/json");
        response.getWriter().write(missingIdentity?"{\"detail\":\"OIDC identity provider is required when demo mode is disabled\"}":"{\"detail\":\"Access denied\"}");
    }
}