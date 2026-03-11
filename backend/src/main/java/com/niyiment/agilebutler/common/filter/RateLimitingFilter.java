package com.niyiment.agilebutler.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter implements Filter {

    private final RedisTemplate<String, Long> rateLimiterRedisTemplate;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitingEnabled;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        if (!rateLimitingEnabled) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String clientIP = getClientIP(request);
        String key = "rate_limit:" + clientIP;

        try {
            Long currentCount = rateLimiterRedisTemplate.opsForValue().increment(key);
            
            if (currentCount != null && currentCount == 1) {
                rateLimiterRedisTemplate.expire(key, 1, TimeUnit.MINUTES);
            }

            if (currentCount != null && currentCount > requestsPerMinute) {
                log.warn("Rate limit exceeded for IP: {}", clientIP);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}