package com.mok.infrastructure.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("rate.limit")
public class RateLimitProperties {

    /**
     * Whether to enable rate limiting
     */
    private boolean enabled = true;

    /**
     * Limit time, in seconds
     */
    private int time = 1;

    /**
     * Limit count
     */
    private int count = 30;
}
