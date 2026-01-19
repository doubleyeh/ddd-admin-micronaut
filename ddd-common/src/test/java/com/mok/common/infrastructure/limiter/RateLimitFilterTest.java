package com.mok.common.infrastructure.limiter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private RateLimiter defaultLimiter;

    @Mock
    private RateLimiter highLimiter;

    @Mock
    private RateLimiter sensitiveLimiter;

    private RateLimitConfig config;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(rateLimiterRegistry.rateLimiter("default")).thenReturn(defaultLimiter);
        when(rateLimiterRegistry.rateLimiter("high")).thenReturn(highLimiter);
        when(rateLimiterRegistry.rateLimiter("sensitive")).thenReturn(sensitiveLimiter);

        config = new RateLimitConfig();
        config.setMappings(Map.of(
            "/api/seckill/**", "high",
            "/api/users:post", "sensitive"
        ));

        filter = new RateLimitFilter(rateLimiterRegistry, config);
    }

    /**
     * 测试正常请求 - 使用默认限流器
     * 期望：返回 200 OK 状态码，默认限流器被调用
     */
    @Test
    void testNormalRequest_DefaultLimiter() {
        HttpRequest<?> request = HttpRequest.GET("/api/other");
        when(defaultLimiter.acquirePermission()).thenReturn(true);

        ServerFilterChain chain = req -> Mono.just(HttpResponse.ok());
        Mono<MutableHttpResponse<?>> mono = (Mono<MutableHttpResponse<?>>) filter.doFilter(request, chain);
        MutableHttpResponse<?> response = mono.block();

        assertEquals(HttpStatus.OK, response.getStatus());
        verify(defaultLimiter).acquirePermission();
    }

    /**
     * 测试高频请求 - 路径匹配 /api/seckill/** 使用 high 限流器
     * 期望：返回 200 OK 状态码，high 限流器被调用
     */
    @Test
    void testHighFrequencyRequest() {
        HttpRequest<?> request = HttpRequest.GET("/api/seckill/product");
        when(highLimiter.acquirePermission()).thenReturn(true);

        ServerFilterChain chain = req -> Mono.just(HttpResponse.ok());
        Mono<MutableHttpResponse<?>> mono = (Mono<MutableHttpResponse<?>>) filter.doFilter(request, chain);
        MutableHttpResponse<?> response = mono.block();

        assertEquals(HttpStatus.OK, response.getStatus());
        verify(highLimiter).acquirePermission();
    }

    /**
     * 测试敏感操作请求 - POST /api/users 使用 sensitive 限流器
     * 期望：返回 200 OK 状态码，sensitive 限流器被调用
     */
    @Test
    void testSensitiveRequest() {
        HttpRequest<?> request = HttpRequest.POST("/api/users", "{}");
        when(sensitiveLimiter.acquirePermission()).thenReturn(true);

        ServerFilterChain chain = req -> Mono.just(HttpResponse.ok());
        Mono<MutableHttpResponse<?>> mono = (Mono<MutableHttpResponse<?>>) filter.doFilter(request, chain);
        MutableHttpResponse<?> response = mono.block();

        assertEquals(HttpStatus.OK, response.getStatus());
        verify(sensitiveLimiter).acquirePermission();
    }

    /**
     * 测试限流拒绝请求的情况 - 默认限流器
     * 期望：返回 429 TOO_MANY_REQUESTS 状态码
     */
    @Test
    void testRateLimited() {
        HttpRequest<?> request = HttpRequest.GET("/api/users");
        when(defaultLimiter.acquirePermission()).thenReturn(false);

        ServerFilterChain chain = req -> Mono.just(HttpResponse.ok());
        Mono<MutableHttpResponse<?>> mono = (Mono<MutableHttpResponse<?>>) filter.doFilter(request, chain);
        MutableHttpResponse<?> response = mono.block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatus());
        verify(defaultLimiter).acquirePermission();
    }
}