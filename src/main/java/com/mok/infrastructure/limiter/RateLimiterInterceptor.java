package com.mok.infrastructure.limiter;

import com.mok.application.exception.BizException;
import com.mok.infrastructure.config.RateLimitProperties;
import com.mok.infrastructure.util.SysUtil;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
@InterceptorBean(RateLimiter.class)
@RequiredArgsConstructor
public class RateLimiterInterceptor implements MethodInterceptor<Object, Object> {

    private final RedisCommands<String, String> redisCommands;
    private final RateLimitProperties rateLimitProperties;

    private static final String LUA_SCRIPT = """
            local c
            c = redis.call('get',KEYS[1])
            if c and tonumber(c) > tonumber(ARGV[1]) then
            return c;
            end
            c = redis.call('incr',KEYS[1])
            if tonumber(c) == 1 then
            redis.call('expire',KEYS[1],ARGV[2])
            end
            return c;""";

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        if (!rateLimitProperties.isEnabled()) {
            return context.proceed();
        }

        Optional<AnnotationValue<RateLimiter>> rateLimiter = context.findAnnotation(RateLimiter.class);

        int time = 1;
        int count = 30;
        String keyPrefix = "rate_limit:";
        LimitType limitType = LimitType.DEFAULT;
        if (rateLimiter.isPresent()) {
            time = rateLimiter.get().get("time", Integer.class).orElse(1);
            count = rateLimiter.get().get("count", Integer.class).orElse(30);
            keyPrefix = rateLimiter.get().get("key", String.class).orElse("rate_limit:");
            limitType = rateLimiter.get().enumValue("limitType", LimitType.class).orElse(LimitType.DEFAULT);
        }

        String key = keyPrefix;
        if (limitType == LimitType.IP) {
            Optional<HttpRequest<Object>> requestOpt = ServerRequestContext.currentRequest();
            if (requestOpt.isPresent()) {
                key += SysUtil.getIpAddress(requestOpt.get()) + ":";
            } else {
                log.warn("Could not get HttpRequest for IP-based rate limiting. Falling back to method name.");
            }
        }
        key += context.getExecutableMethod().getMethodName();

        List<String> keys = Collections.singletonList(key);

        Long currentCount = redisCommands.eval(LUA_SCRIPT, ScriptOutputType.INTEGER, keys.toArray(new String[0]), String.valueOf(count), String.valueOf(time));

        if (currentCount != null && currentCount.intValue() <= count) {
            log.info("第{}次访问key为 {}，描述为 [{}] 的接口", currentCount, keys, keyPrefix);
            return context.proceed();
        } else {
            throw new BizException("系统繁忙，请稍后重试");
        }
    }
}
