package com.mok.infrastructure.limiter;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Type;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Around
@Type(RateLimiterInterceptor.class)
public @interface RateLimiter {

    /**
     * 限流key
     */
    String key() default "rate_limit:";

    /**
     * 限流时间,单位秒
     */
    int time() default 1;

    /**
     * 限流次数
     */
    int count() default 30;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;
}
