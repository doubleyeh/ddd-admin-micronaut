package com.mok.infrastructure.limiter;

import io.micronaut.context.annotation.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties("rate-limit")
public class RateLimitConfig {

    private Map<String, String> mappings;

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }
}