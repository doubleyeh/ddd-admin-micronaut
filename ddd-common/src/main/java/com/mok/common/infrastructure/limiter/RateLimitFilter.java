package com.mok.common.infrastructure.limiter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Map;

@Filter("/**")
@Singleton
public class RateLimitFilter implements HttpServerFilter {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final RateLimitConfig config;

    public RateLimitFilter(RateLimiterRegistry rateLimiterRegistry, RateLimitConfig config) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.config = config;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String path = request.getPath();
        String method = request.getMethodName();

        String rateLimiterName = "default"; // 默认

        // 检查映射配置
        if (config.getMappings() != null) {
            for (Map.Entry<String, String> entry : config.getMappings().entrySet()) {
                String key = entry.getKey();
                if (key.contains(":")) {
                    // 带方法的映射，如 "/api/users:post"
                    String[] parts = key.split(":");
                    if (parts.length == 2 && path.startsWith(parts[0]) && method.equalsIgnoreCase(parts[1])) {
                        rateLimiterName = entry.getValue();
                        break;
                    }
                } else {
                    // 路径匹配，如 "/api/seckill/**"
                    if (path.startsWith(key.replace("/**", ""))) {
                        rateLimiterName = entry.getValue();
                        break;
                    }
                }
            }
        }

        // 验证限流器名称，只允许预定义的
        if (!"default".equals(rateLimiterName) && !"high".equals(rateLimiterName) && !"sensitive".equals(rateLimiterName)) {
            rateLimiterName = "default";
        }

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);

        if (rateLimiter.acquirePermission()) {
            return chain.proceed(request);
        } else {
            return Mono.just(HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS));
        }
    }
}