package com.fashion.reviewservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class ReviewSecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/reviews/product/**",
            "/actuator/**"
    };

    @Value("${security.internal.caller:chatbot-service}")
    private String trustedInternalCaller;

    @Value("${security.internal.shared-secret:local-chatbot-internal-secret}")
    private String trustedInternalSharedSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable) // CORS handled at API Gateway
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().access(hasGatewayIdentity())
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> hasGatewayIdentity() {
        return (authentication, context) -> {
            String userId = context.getRequest().getHeader("X-User-Id");
            String userRole = context.getRequest().getHeader("X-User-Role");
            String internalCaller = context.getRequest().getHeader("X-Internal-Caller");
            String internalAuth = context.getRequest().getHeader("X-Internal-Auth");
            boolean trustedInternal = trustedInternalCaller.equals(internalCaller)
                    && trustedInternalSharedSecret.equals(internalAuth);
            boolean allowed = StringUtils.hasText(userId) || StringUtils.hasText(userRole) || trustedInternal;
            return new AuthorizationDecision(allowed);
        };
    }
}

