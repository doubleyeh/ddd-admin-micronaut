package com.mok.application.sys.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mok.application.sys.dto.tenant.TenantDTO;
import com.mok.application.sys.mapper.TenantMapper;
import com.mok.domain.sys.repository.TenantRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Singleton
@RequiredArgsConstructor
public class TenantCacheService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final RedisCommands<String, String> redisCommands;
    private final ObjectMapper objectMapper;

    public TenantDTO findByTenantId(@NonNull String tenantId) {
        String key = Const.CacheKey.TENANT + tenantId;
        String cached = redisCommands.get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, TenantDTO.class);
            } catch (IOException e) {
                // Log error or handle
            }
        }

        TenantDTO dto = tenantRepository.findByTenantId(tenantId)
                .map(tenantMapper::toDto)
                .orElse(null);

        if (dto != null) {
            try {
                redisCommands.set(key, objectMapper.writeValueAsString(dto));
            } catch (JsonProcessingException e) {
                // Log error or handle
            }
        }
        return dto;
    }
}
