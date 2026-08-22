package com.ledgerflow.ratelimit.filter;

import com.ledgerflow.ratelimit.service.RedisSlidingWindowRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisSlidingWindowRateLimiter rateLimiter;

    @Value("${ledgerflow.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${ledgerflow.rate-limit.payment-limit-per-minute:10}")
    private int paymentLimitPerMinute;

    @Value("${ledgerflow.rate-limit.order-limit-per-minute:60}")
    private int orderLimitPerMinute;

    @Value("${ledgerflow.rate-limit.default-limit-per-minute:120}")
    private int defaultLimitPerMinute;

    public RateLimitingFilter(RedisSlidingWindowRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate limit API mutations and endpoints
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String tier;
        int limit;

        if (path.startsWith("/api/payments") && "POST".equalsIgnoreCase(method)) {
            tier = "payments";
            limit = paymentLimitPerMinute;
        } else if (path.startsWith("/api/orders") && "POST".equalsIgnoreCase(method)) {
            tier = "orders";
            limit = orderLimitPerMinute;
        } else {
            tier = "general";
            limit = defaultLimitPerMinute;
        }

        RedisSlidingWindowRateLimiter.RateLimitResult result = rateLimiter.isAllowed(tier, clientIp, limit);

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetSeconds()));

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                      "status": 429,
                      "code": "RATE_LIMIT_EXCEEDED",
                      "message": "Rate limit exceeded for endpoint. Please retry after backoff."
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
