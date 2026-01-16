package com.mok.application.sys.service;

import com.mok.application.exception.BizException;
import com.mok.application.sys.dto.tenant.TenantCreateResultDTO;
import com.mok.application.sys.dto.tenant.TenantDTO;
import com.mok.application.sys.dto.tenant.TenantSaveDTO;
import com.mok.application.sys.event.TenantCreatedEvent;
import com.mok.application.sys.mapper.TenantMapper;
import com.mok.domain.sys.model.Tenant;
import com.mok.domain.sys.repository.TenantRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantServiceTest {

    private TenantRepository tenantRepository;
    private TenantMapper tenantMapper;
    private ApplicationEventPublisher<TenantCreatedEvent> eventPublisher;
    private RedisCommands<String, String> redisCommands;
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        tenantMapper = mock(TenantMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        redisCommands = mock(RedisCommands.class);
        tenantService = new TenantService(tenantRepository, tenantMapper, eventPublisher, redisCommands);
    }

    @Test
    void createTenant_Success() {
        TenantSaveDTO dto = new TenantSaveDTO();
        dto.setName("New Tenant");
        dto.setPackageId(1L);

        Tenant savedTenant = Tenant.create(dto.getName(), dto.getContactPerson(), dto.getContactPhone(), dto.getPackageId(), tenantRepository);
        savedTenant.setId(1L);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);

        TenantCreateResultDTO result = tenantService.createTenant(dto);

        assertNotNull(result);
        assertNotNull(result.getTenantId());
        assertEquals("New Tenant", result.getName());
        assertNotNull(result.getInitialAdminPassword());

        ArgumentCaptor<TenantCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TenantCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TenantCreatedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.getTenant().getTenantId());
        assertEquals(result.getInitialAdminPassword(), publishedEvent.getRawPassword());
    }

    @Test
    void updateTenant_Success() {
        Long tenantId = 1L;
        TenantSaveDTO dto = new TenantSaveDTO();
        dto.setTenantId("T123");
        dto.setName("Updated Name");
        dto.setPackageId(2L);

        Tenant existingTenant = Tenant.create("Old Name", null, null, 1L, tenantRepository);
        existingTenant.setTenantId("T123"); // Manually set for test consistency
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(existingTenant);

        tenantService.updateTenant(tenantId, dto);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        Tenant savedTenant = captor.getValue();

        assertEquals("Updated Name", savedTenant.getName());
        assertEquals(2L, savedTenant.getPackageId());

        verify(redisCommands).del(Const.CacheKey.TENANT + "T123");
    }

    @Test
    void updateTenant_ChangingTenantId_ShouldThrowBizException() {
        Long tenantId = 1L;
        TenantSaveDTO dto = new TenantSaveDTO();
        dto.setTenantId("T456");

        Tenant existingTenant = Tenant.create("Old Name", null, null, 1L, tenantRepository);
        existingTenant.setTenantId("T123");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingTenant));

        Exception exception = assertThrows(BizException.class, () -> tenantService.updateTenant(tenantId, dto));
        assertEquals("租户编码不可修改", exception.getMessage());
    }

    @Test
    void deleteByVerify_Success() {
        Long tenantId = 1L;
        Tenant tenant = Tenant.create("Test", null, null, 1L, tenantRepository);
        tenant.setTenantId("T123");

        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setTenantId("T123");

        when(tenantMapper.toDto(any(Tenant.class))).thenReturn(tenantDTO);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        boolean result = tenantService.deleteByVerify(tenantId);

        assertTrue(result);
        verify(tenantRepository).deleteById(tenantId);
        verify(redisCommands).del(Const.CacheKey.TENANT + "T123");
    }

    @Test
    void deleteByVerify_SuperTenant_ShouldThrowBizException() {
        Long tenantId = 1L;
        Tenant tenant = Tenant.create("Super", null, null, 1L, tenantRepository);
        tenant.setTenantId(Const.SUPER_TENANT_ID);
        
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setTenantId(Const.SUPER_TENANT_ID);

        when(tenantMapper.toDto(any(Tenant.class))).thenReturn(tenantDTO);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        Exception exception = assertThrows(BizException.class, () -> tenantService.deleteByVerify(tenantId));
        assertEquals("该租户不可删除", exception.getMessage());
    }
}
