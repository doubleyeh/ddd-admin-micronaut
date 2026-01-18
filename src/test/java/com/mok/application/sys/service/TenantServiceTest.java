package com.mok.application.sys.service;

import com.mok.application.exception.BizException;
import com.mok.application.sys.dto.tenant.*;
import com.mok.application.sys.event.TenantCreatedEvent;
import com.mok.application.sys.mapper.TenantMapper;
import com.mok.domain.sys.model.Tenant;
import com.mok.domain.sys.repository.TenantRepository;
import com.mok.infrastructure.common.Const;
import com.mok.infrastructure.util.PasswordGenerator;
import com.mok.infrastructure.util.SysUtil;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    
    @Mock
    private TenantMapper tenantMapper;
    
    @Mock
    private ApplicationEventPublisher<TenantCreatedEvent> eventPublisher;
    
    @Mock
    private RedisCommands<String, String> redisCommands;
    
    @Mock
    private PasswordGenerator passwordGenerator;
    
    private TenantService tenantService;
    private MockedStatic<SysUtil> sysUtilMock;
    private final Long TEST_TENANT_ID = 1L;
    private final String TEST_TENANT_CODE = "TEST123";
    private final String TEST_TENANT_NAME = "Test Tenant";
    private final String TEST_CONTACT_PERSON = "John Doe";
    private final String TEST_CONTACT_PHONE = "1234567890";
    private final Long TEST_PACKAGE_ID = 1L;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepository, tenantMapper, eventPublisher, redisCommands);
        sysUtilMock = mockStatic(SysUtil.class);
    }

    @AfterEach
    void tearDown() {
        sysUtilMock.close();
    }

    @Test
    void findPage_Success() {
        // Given
        TenantQuery query = new TenantQuery();
        query.setName("test");
        query.setState(Const.TenantState.NORMAL);
        
        Pageable pageable = mock(Pageable.class);
        Page<Tenant> entityPage = Page.empty();
        
        when(tenantRepository.findTenantPage(any(), eq(pageable))).thenAnswer(invocation -> {
            // Service returns entityPage.map(tenantMapper::toDto), so we need to mock that behavior
            return entityPage.map(tenantMapper::toDto);
        });
        
        // When
        Page<TenantDTO> result = tenantService.findPage(query, pageable);
        
        // Then
        assertNotNull(result);
        verify(tenantRepository).findTenantPage(any(), eq(pageable));
    }

    @Test
    void getById_Success() {
        // Given
        Tenant tenant = Tenant.create(TEST_TENANT_NAME, TEST_CONTACT_PERSON, TEST_CONTACT_PHONE, TEST_PACKAGE_ID, tenantRepository);
        tenant.setId(TEST_TENANT_ID);
        tenant.setTenantId(TEST_TENANT_CODE);
        
        TenantDTO expectedDto = new TenantDTO();
        expectedDto.setId(TEST_TENANT_ID);
        expectedDto.setTenantId(TEST_TENANT_CODE);
        expectedDto.setName(TEST_TENANT_NAME);
        
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantMapper.toDto(tenant)).thenReturn(expectedDto);

        // When
        TenantDTO result = tenantService.getById(TEST_TENANT_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TENANT_ID, result.getId());
        assertEquals(TEST_TENANT_CODE, result.getTenantId());
        assertEquals(TEST_TENANT_NAME, result.getName());
        
        verify(tenantRepository).findById(TEST_TENANT_ID);
        verify(tenantMapper).toDto(tenant);
    }

    @Test
    void getById_TenantNotFound_ShouldThrowBizException() {
        // Given
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> tenantService.getById(TEST_TENANT_ID));
        assertEquals("租户不存在", exception.getMessage());
        
        verify(tenantRepository).findById(TEST_TENANT_ID);
        verify(tenantMapper, never()).toDto(any());
    }

    @Test
    void createTenant_Success() {
        // Given
        TenantSaveDTO dto = new TenantSaveDTO();
        dto.setName(TEST_TENANT_NAME);
        dto.setContactPerson(TEST_CONTACT_PERSON);
        dto.setContactPhone(TEST_CONTACT_PHONE);
        dto.setPackageId(TEST_PACKAGE_ID);
        
        String rawPassword = "rawPassword123";
        
        Tenant savedTenant = Tenant.create(dto.getName(), dto.getContactPerson(), dto.getContactPhone(), dto.getPackageId(), tenantRepository);
        savedTenant.setId(TEST_TENANT_ID);
        savedTenant.setTenantId(TEST_TENANT_CODE);
        
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        try (MockedStatic<PasswordGenerator> passwordGeneratorMock = mockStatic(PasswordGenerator.class)) {
            passwordGeneratorMock.when(PasswordGenerator::generateRandomPassword).thenReturn(rawPassword);
            
            // When
            TenantCreateResultDTO result = tenantService.createTenant(dto);

            // Then
            assertNotNull(result);
            assertEquals(TEST_TENANT_ID, result.getId());
            assertEquals(TEST_TENANT_CODE, result.getTenantId());
            assertEquals(TEST_TENANT_NAME, result.getName());
            assertEquals(TEST_CONTACT_PERSON, result.getContactPerson());
            assertEquals(TEST_CONTACT_PHONE, result.getContactPhone());
            assertEquals(Const.TenantState.NORMAL, result.getState());
            assertEquals(rawPassword, result.getInitialAdminPassword());

            verify(tenantRepository).save(any(Tenant.class));
            passwordGeneratorMock.verify(PasswordGenerator::generateRandomPassword);
        }
    }

    @Test
    void updateTenant_Success() {
        // Given
        TenantSaveDTO dto = new TenantSaveDTO();
        dto.setTenantId(TEST_TENANT_CODE); // Same tenant ID to avoid exception
        dto.setName("Updated Name");
        dto.setContactPerson("Updated Contact");
        dto.setContactPhone("9876543210");
        dto.setPackageId(2L);

        Tenant existingTenant = Tenant.create(TEST_TENANT_NAME, TEST_CONTACT_PERSON, TEST_CONTACT_PHONE, TEST_PACKAGE_ID, tenantRepository);
        existingTenant.setId(TEST_TENANT_ID);
        existingTenant.setTenantId(TEST_TENANT_CODE);
        
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantMapper.toDto(any(Tenant.class))).thenReturn(new TenantDTO());

        // When
        TenantDTO result = tenantService.updateTenant(TEST_TENANT_ID, dto);

        // Then
        assertNotNull(result);
        
        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        Tenant savedTenant = captor.getValue();
        
        assertEquals("Updated Name", savedTenant.getName());
        assertEquals("Updated Contact", savedTenant.getContactPerson());
        assertEquals("9876543210", savedTenant.getContactPhone());
        assertEquals(Long.valueOf(2), savedTenant.getPackageId());
        
        verify(redisCommands).del(Const.CacheKey.TENANT + TEST_TENANT_CODE);
    }

    @Test
    void updateTenant_TenantNotFound_ShouldThrowBizException() {
        // Given
        TenantSaveDTO dto = new TenantSaveDTO();
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> tenantService.updateTenant(TEST_TENANT_ID, dto));
        assertEquals("租户不存在", exception.getMessage());
        
        verify(tenantRepository, never()).save(any());
        verify(redisCommands, never()).del(anyString());
    }

    @Test
    void updateTenant_ChangingTenantId_ShouldThrowBizException() {
        // Given
        TenantSaveDTO dto = new TenantSaveDTO();
        dto.setTenantId("DIFFERENT_CODE"); // Different tenant ID

        Tenant existingTenant = Tenant.create(TEST_TENANT_NAME, TEST_CONTACT_PERSON, TEST_CONTACT_PHONE, TEST_PACKAGE_ID, tenantRepository);
        existingTenant.setId(TEST_TENANT_ID);
        existingTenant.setTenantId(TEST_TENANT_CODE);
        
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.of(existingTenant));

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> tenantService.updateTenant(TEST_TENANT_ID, dto));
        assertEquals("租户编码不可修改", exception.getMessage());
        
        verify(tenantRepository, never()).save(any());
        verify(redisCommands, never()).del(anyString());
    }

    @Test
    void updateTenantState_Enable_Success() {
        // Given
        Tenant existingTenant = Tenant.create(TEST_TENANT_NAME, TEST_CONTACT_PERSON, TEST_CONTACT_PHONE, TEST_PACKAGE_ID, tenantRepository);
        existingTenant.setId(TEST_TENANT_ID);
        existingTenant.setTenantId(TEST_TENANT_CODE);
        existingTenant.disable(); // Start with disabled state
        
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantMapper.toDto(any(Tenant.class))).thenReturn(new TenantDTO());

        // When
        TenantDTO result = tenantService.updateTenantState(TEST_TENANT_ID, Const.TenantState.NORMAL);

        // Then
        assertNotNull(result);
        
        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        Tenant savedTenant = captor.getValue();
        
        assertEquals(Const.TenantState.NORMAL, savedTenant.getState());
        verify(redisCommands).del(Const.CacheKey.TENANT + TEST_TENANT_CODE);
    }

    @Test
    void updateTenantState_Disable_Success() {
        // Given
        Tenant existingTenant = Tenant.create(TEST_TENANT_NAME, TEST_CONTACT_PERSON, TEST_CONTACT_PHONE, TEST_PACKAGE_ID, tenantRepository);
        existingTenant.setId(TEST_TENANT_ID);
        existingTenant.setTenantId(TEST_TENANT_CODE);
        existingTenant.enable(); // Start with normal state
        
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantMapper.toDto(any(Tenant.class))).thenReturn(new TenantDTO());

        // When
        TenantDTO result = tenantService.updateTenantState(TEST_TENANT_ID, Const.TenantState.DISABLED);

        // Then
        assertNotNull(result);
        
        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        Tenant savedTenant = captor.getValue();
        
        assertEquals(Const.TenantState.DISABLED, savedTenant.getState());
        verify(redisCommands).del(Const.CacheKey.TENANT + TEST_TENANT_CODE);
    }

    @Test
    void updateTenantState_InvalidState_ShouldThrowBizException() {
        // Given
        Integer invalidState = 999;
        
        Tenant existingTenant = Tenant.create(TEST_TENANT_NAME, TEST_CONTACT_PERSON, TEST_CONTACT_PHONE, TEST_PACKAGE_ID, tenantRepository);
        existingTenant.setId(TEST_TENANT_ID);
        existingTenant.setTenantId(TEST_TENANT_CODE);
        
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.of(existingTenant));

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> tenantService.updateTenantState(TEST_TENANT_ID, invalidState));
        assertEquals("无效的状态值: " + invalidState, exception.getMessage());
        
        verify(tenantRepository, never()).save(any());
        verify(redisCommands, never()).del(anyString());
    }

    @Test
    void updateTenantState_TenantNotFound_ShouldThrowBizException() {
        // Given
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> tenantService.updateTenantState(TEST_TENANT_ID, Const.TenantState.NORMAL));
        assertEquals("租户不存在", exception.getMessage());
        
        verify(tenantRepository, never()).save(any());
        verify(redisCommands, never()).del(anyString());
    }

    @Test
    void deleteByVerify_Success() {
        // Given
        Tenant tenant = Tenant.create(TEST_TENANT_NAME, TEST_CONTACT_PERSON, TEST_CONTACT_PHONE, TEST_PACKAGE_ID, tenantRepository);
        tenant.setId(TEST_TENANT_ID);
        tenant.setTenantId(TEST_TENANT_CODE);
        
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setTenantId(TEST_TENANT_CODE);
        
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantMapper.toDto(tenant)).thenReturn(tenantDTO);
        
        sysUtilMock.when(() -> SysUtil.isSuperTenant(TEST_TENANT_CODE)).thenReturn(false);

        // When
        boolean result = tenantService.deleteByVerify(TEST_TENANT_ID);

        // Then
        assertTrue(result);
        verify(tenantRepository).deleteById(TEST_TENANT_ID);
        verify(redisCommands).del(Const.CacheKey.TENANT + TEST_TENANT_CODE);
    }

    @Test
    void deleteByVerify_TenantNotFound_ShouldThrowBizException() {
        // Given
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> tenantService.deleteByVerify(TEST_TENANT_ID));
        assertEquals("租户不存在", exception.getMessage());
        
        verify(tenantRepository, never()).deleteById(any());
        verify(redisCommands, never()).del(anyString());
    }

    @Test
    void deleteByVerify_SuperTenant_ShouldThrowBizException() {
        // Given
        Tenant tenant = Tenant.create(TEST_TENANT_NAME, TEST_CONTACT_PERSON, TEST_CONTACT_PHONE, TEST_PACKAGE_ID, tenantRepository);
        tenant.setId(TEST_TENANT_ID);
        tenant.setTenantId(Const.SUPER_TENANT_ID);
        
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setTenantId(Const.SUPER_TENANT_ID);
        
        when(tenantRepository.findById(TEST_TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantMapper.toDto(tenant)).thenReturn(tenantDTO);
        
        sysUtilMock.when(() -> SysUtil.isSuperTenant(Const.SUPER_TENANT_ID)).thenReturn(true);

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> tenantService.deleteByVerify(TEST_TENANT_ID));
        assertEquals("该租户不可删除", exception.getMessage());
        
        verify(tenantRepository, never()).deleteById(any());
        verify(redisCommands, never()).del(anyString());
    }

    @Test
    void findOptions_WithName_Success() {
        // Given
        String searchName = "test";
        
        Tenant tenant1 = Tenant.create("Tenant 1", null, null, 1L, tenantRepository);
        tenant1.setTenantId("T1");
        tenant1.enable();
        Tenant tenant2 = Tenant.create("Tenant 2", null, null, 2L, tenantRepository);
        tenant2.setTenantId("T2");
        tenant2.enable();
        
        List<Tenant> tenants = List.of(tenant1, tenant2);
        when(tenantRepository.findByNameContainsIgnoreCaseAndState(searchName, Const.TenantState.NORMAL))
            .thenReturn(tenants);
        
        TenantDTO dto1 = new TenantDTO();
        dto1.setId(1L);
        dto1.setTenantId("T1");
        dto1.setName("Tenant 1");
        TenantDTO dto2 = new TenantDTO();
        dto2.setId(2L);
        dto2.setTenantId("T2");
        dto2.setName("Tenant 2");
        
        when(tenantMapper.toDto(tenant1)).thenReturn(dto1);
        when(tenantMapper.toDto(tenant2)).thenReturn(dto2);

        // When
        List<TenantOptionDTO> result = tenantService.findOptions(searchName);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("T1", result.get(0).getTenantId());
        assertEquals("Tenant 1", result.get(0).getName());
        assertEquals(2L, result.get(1).getId());
        assertEquals("T2", result.get(1).getTenantId());
        assertEquals("Tenant 2", result.get(1).getName());
        
        verify(tenantRepository).findByNameContainsIgnoreCaseAndState(searchName, Const.TenantState.NORMAL);
        verify(tenantMapper).toDto(tenant1);
        verify(tenantMapper).toDto(tenant2);
    }

    @Test
    void findOptions_WithoutName_Success() {
        // Given
        Tenant tenant = Tenant.create(TEST_TENANT_NAME, null, null, 1L, tenantRepository);
        tenant.setTenantId(TEST_TENANT_CODE);
        tenant.enable();
        
        List<Tenant> tenants = List.of(tenant);
        when(tenantRepository.findByState(Const.TenantState.NORMAL)).thenReturn(tenants);
        
        TenantDTO dto = new TenantDTO();
        dto.setId(TEST_TENANT_ID);
        dto.setTenantId(TEST_TENANT_CODE);
        dto.setName(TEST_TENANT_NAME);
        
        when(tenantMapper.toDto(tenant)).thenReturn(dto);

        // When
        List<TenantOptionDTO> result = tenantService.findOptions(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_TENANT_ID, result.get(0).getId());
        assertEquals(TEST_TENANT_CODE, result.get(0).getTenantId());
        assertEquals(TEST_TENANT_NAME, result.get(0).getName());
        
        verify(tenantRepository).findByState(Const.TenantState.NORMAL);
        verify(tenantMapper).toDto(tenant);
    }
}