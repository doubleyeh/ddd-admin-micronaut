package com.mok.common.infrastructure.config;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class RedisFactory {

    @Singleton
    public RedisCommands<String, String> syncCommands(StatefulRedisConnection<String, String> connection) {
        return connection.sync();
    }
}