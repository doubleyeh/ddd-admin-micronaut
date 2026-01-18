package com.mok.application.sys.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mok.application.sys.dto.tenant.TenantDTO;
import com.mok.application.sys.mapper.TenantMapper;
import com.mok.domain.sys.model.Tenant;
import com.mok.domain.sys.repository.TenantRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantCacheServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private RedisCommands<String, String> redisCommands;

    @Mock
    private ObjectMapper objectMapper;

    private TenantCacheService tenantCacheService;
    private final String TEST_TENANT_ID = "test123";
    private final String CACHE_KEY = "sys:tenant:test123";
    private final String CACHED_JSON =
        "{\"id\":1,\"tenantId\":\"test123\",\"name\":\"Test Tenant\"}";

    @BeforeEach
    void setUp() {
        tenantCacheService = new TenantCacheService(
            tenantRepository,
            tenantMapper,
            redisCommands,
            objectMapper
        );
    }

    @Test
    void findByTenantId_CacheHit_Success() throws IOException {
        // Given
        TenantDTO expectedDto = new TenantDTO();
        expectedDto.setTenantId(TEST_TENANT_ID);
        expectedDto.setName("Test Tenant");

        when(redisCommands.get(CACHE_KEY)).thenReturn(CACHED_JSON);
        when(objectMapper.readValue(CACHED_JSON, TenantDTO.class)).thenReturn(
            expectedDto
        );

        // When
        TenantDTO result = tenantCacheService.findByTenantId(TEST_TENANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals("Test Tenant", result.getName());

        verify(redisCommands).get(CACHE_KEY);
        verify(objectMapper).readValue(CACHED_JSON, TenantDTO.class);
        verifyNoInteractions(tenantRepository);
        verifyNoInteractions(tenantMapper);
    }

    @Test
    void findByTenantId_CacheHit_RuntimeException_ShouldFallbackToDatabase()
        throws IOException {
        // Given
        Tenant tenant = mock(Tenant.class);
        TenantDTO expectedDto = new TenantDTO();
        expectedDto.setTenantId(TEST_TENANT_ID);
        expectedDto.setName("Test Tenant");

        when(redisCommands.get(CACHE_KEY)).thenReturn(CACHED_JSON);
        doThrow(new JsonProcessingException("Invalid JSON") {})
            .when(objectMapper)
            .readValue(CACHED_JSON, TenantDTO.class);
        when(tenantRepository.findByTenantId(TEST_TENANT_ID)).thenReturn(
            Optional.of(tenant)
        );
        when(tenantMapper.toDto(tenant)).thenReturn(expectedDto);

        // When
        TenantDTO result = tenantCacheService.findByTenantId(TEST_TENANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals("Test Tenant", result.getName());

        verify(redisCommands).get(CACHE_KEY);
        verify(objectMapper).readValue(CACHED_JSON, TenantDTO.class);
        verify(tenantRepository).findByTenantId(TEST_TENANT_ID);
        verify(tenantMapper).toDto(tenant);
    }

    @Test
    void findByTenantId_CacheMiss_DatabaseHit_Success() throws Exception {
        // Given
        Tenant tenant = mock(Tenant.class);
        TenantDTO expectedDto = new TenantDTO();
        expectedDto.setTenantId(TEST_TENANT_ID);
        expectedDto.setName("Test Tenant");

        when(redisCommands.get(CACHE_KEY)).thenReturn(null);
        when(tenantRepository.findByTenantId(TEST_TENANT_ID)).thenReturn(
            Optional.of(tenant)
        );
        when(tenantMapper.toDto(tenant)).thenReturn(expectedDto);
        when(objectMapper.writeValueAsString(expectedDto)).thenReturn(
            CACHED_JSON
        );

        // When
        TenantDTO result = tenantCacheService.findByTenantId(TEST_TENANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals("Test Tenant", result.getName());

        verify(redisCommands).get(CACHE_KEY);
        verify(tenantRepository).findByTenantId(TEST_TENANT_ID);
        verify(tenantMapper).toDto(tenant);
        verify(redisCommands).set(CACHE_KEY, CACHED_JSON);
        verify(objectMapper).writeValueAsString(expectedDto);
    }

    @Test
    void findByTenantId_CacheMiss_DatabaseHit_JsonException_ShouldStillReturnDto()
        throws Exception {
        // Given
        Tenant tenant = mock(Tenant.class);
        TenantDTO expectedDto = new TenantDTO();
        expectedDto.setTenantId(TEST_TENANT_ID);
        expectedDto.setName("Test Tenant");

        when(redisCommands.get(CACHE_KEY)).thenReturn(null);
        when(tenantRepository.findByTenantId(TEST_TENANT_ID)).thenReturn(
            Optional.of(tenant)
        );
        when(tenantMapper.toDto(tenant)).thenReturn(expectedDto);
        when(objectMapper.writeValueAsString(expectedDto)).thenThrow(
            new JsonProcessingException("Serialization error") {}
        );

        // When
        TenantDTO result = tenantCacheService.findByTenantId(TEST_TENANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals("Test Tenant", result.getName());

        verify(redisCommands).get(CACHE_KEY);
        verify(tenantRepository).findByTenantId(TEST_TENANT_ID);
        verify(tenantMapper).toDto(tenant);
        verify(objectMapper).writeValueAsString(expectedDto);
        verify(redisCommands, never()).set(anyString(), anyString());
    }

    @Test
    void findByTenantId_EmptyTenantId_ShouldWorkNormally() throws Exception {
        // Given
        String emptyTenantId = "";
        String emptyCacheKey = "sys:tenant:";

        Tenant tenant = mock(Tenant.class);
        TenantDTO expectedDto = new TenantDTO();
        expectedDto.setTenantId(emptyTenantId);
        expectedDto.setName("Test Tenant");

        when(redisCommands.get(emptyCacheKey)).thenReturn(null);
        when(tenantRepository.findByTenantId(emptyTenantId)).thenReturn(
            Optional.of(tenant)
        );
        when(tenantMapper.toDto(tenant)).thenReturn(expectedDto);

        // When
        TenantDTO result = tenantCacheService.findByTenantId(emptyTenantId);

        // Then
        assertNotNull(result);
        assertEquals(emptyTenantId, result.getTenantId());

        verify(redisCommands).get(emptyCacheKey);
    }

    @Test
    void findByTenantId_VerifyCacheKeyFormat() {
        // Given
        when(redisCommands.get(anyString())).thenReturn(null);
        when(tenantRepository.findByTenantId(TEST_TENANT_ID)).thenReturn(
            Optional.empty()
        );

        // When
        tenantCacheService.findByTenantId(TEST_TENANT_ID);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(
            String.class
        );
        verify(redisCommands).get(keyCaptor.capture());
        assertEquals(
            Const.CacheKey.TENANT + TEST_TENANT_ID,
            keyCaptor.getValue()
        );
    }
}
