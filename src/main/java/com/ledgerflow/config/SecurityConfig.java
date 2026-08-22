package com.ledgerflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${ledgerflow.security.admin-api-key}")
    private String adminApiKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**",
                                "/api/orders/**",
                                "/api/payments/**",
                                "/api/ledger/**",
                                "/api/webhooks/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").permitAll() // AdminApiKeyFilter handles authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new AdminApiKeyFilter(adminApiKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static class AdminApiKeyFilter extends OncePerRequestFilter {
        private final String expectedApiKey;

        public AdminApiKeyFilter(String expectedApiKey) {
            this.expectedApiKey = expectedApiKey;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String path = request.getRequestURI();
            if (path.startsWith("/api/admin")) {
                String apiKey = request.getHeader("X-API-KEY");
                if (apiKey == null || !apiKey.equals(expectedApiKey)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized - Valid X-API-KEY required for administrative operations\"}");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}
