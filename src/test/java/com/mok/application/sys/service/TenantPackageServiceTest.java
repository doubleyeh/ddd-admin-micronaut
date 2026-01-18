package com.mok.application.sys.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mok.application.exception.BizException;
import com.mok.application.sys.dto.tenantPackage.*;
import com.mok.application.sys.mapper.MenuMapper;
import com.mok.application.sys.mapper.PermissionMapper;
import com.mok.application.sys.mapper.TenantPackageMapper;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.model.Permission;
import com.mok.domain.sys.model.TenantPackage;
import com.mok.domain.sys.repository.MenuRepository;
import com.mok.domain.sys.repository.PermissionRepository;
import com.mok.domain.sys.repository.TenantPackageRepository;
import com.mok.domain.sys.repository.TenantRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantPackageServiceTest {

    @Mock
    private TenantPackageRepository packageRepository;
    
    @Mock
    private MenuRepository menuRepository;
    
    @Mock
    private PermissionRepository permissionRepository;
    
    @Mock
    private TenantPackageMapper packageMapper;
    
    @Mock
    private TenantRepository tenantRepository;
    
    @Mock
    private RedisCommands<String, String> redisCommands;
    
    @Mock
    private MenuMapper menuMapper;
    
    @Mock
    private PermissionMapper permissionMapper;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private TenantPackageService packageService;
    private final Long TEST_PACKAGE_ID = 1L;
    private final String TEST_PACKAGE_NAME = "Test Package";
    private final String TEST_DESCRIPTION = "Test Description";

    @BeforeEach
    void setUp() {
        packageService = new TenantPackageService(
            packageRepository, menuRepository, permissionRepository, packageMapper,
            tenantRepository, redisCommands, menuMapper, permissionMapper, objectMapper
        );
    }

    @Test
    void findPage_Success() {
        // Given
        TenantPackageQuery query = new TenantPackageQuery();
        query.setName("test");
        query.setState(Const.TenantPackageState.NORMAL);
        
        Pageable pageable = mock(Pageable.class);
        Page<TenantPackage> entityPage = Page.empty();
        
        when(packageRepository.findAll(any(), eq(pageable))).thenReturn(entityPage);
        
        // When
        Page<TenantPackageDTO> result = packageService.findPage(query, pageable);
        
        // Then
        assertNotNull(result);
        verify(packageRepository).findAll(any(), eq(pageable));
    }

    @Test
    void createPackage_Success() {
        // Given
        TenantPackageSaveDTO dto = new TenantPackageSaveDTO();
        dto.setName(TEST_PACKAGE_NAME);
        dto.setDescription(TEST_DESCRIPTION);

        // When
        packageService.createPackage(dto);

        // Then
        ArgumentCaptor<TenantPackage> captor = ArgumentCaptor.forClass(TenantPackage.class);
        verify(packageRepository).save(captor.capture());
        TenantPackage savedPackage = captor.getValue();

        assertEquals(TEST_PACKAGE_NAME, savedPackage.getName());
        assertEquals(TEST_DESCRIPTION, savedPackage.getDescription());
        assertEquals(Const.TenantPackageState.NORMAL, savedPackage.getState());
    }

    @Test
    void updatePackage_Success() {
        // Given
        String newName = "Updated Package";
        String newDescription = "Updated Description";
        TenantPackageSaveDTO dto = new TenantPackageSaveDTO();
        dto.setName(newName);
        dto.setDescription(newDescription);

        TenantPackage existingPackage = TenantPackage.create("Old Name", "Old Description");
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(existingPackage));
        when(packageRepository.save(any(TenantPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        packageService.updatePackage(TEST_PACKAGE_ID, dto);

        // Then
        ArgumentCaptor<TenantPackage> captor = ArgumentCaptor.forClass(TenantPackage.class);
        verify(packageRepository).save(captor.capture());
        TenantPackage updatedPackage = captor.getValue();

        assertEquals(newName, updatedPackage.getName());
        assertEquals(newDescription, updatedPackage.getDescription());
    }

    @Test
    void updatePackage_PackageNotFound_ShouldThrowBizException() {
        // Given
        TenantPackageSaveDTO dto = new TenantPackageSaveDTO();
        dto.setName("Updated Package");
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> packageService.updatePackage(TEST_PACKAGE_ID, dto));
        assertEquals("套餐不存在", exception.getMessage());
        
        verify(packageRepository, never()).save(any());
    }

    @Test
    void grant_Success() {
        // Given
        TenantPackageGrantDTO dto = new TenantPackageGrantDTO();
        dto.setMenuIds(Set.of(10L));
        dto.setPermissionIds(Set.of(100L));

        TenantPackage existingPackage = TenantPackage.create(TEST_PACKAGE_NAME, "");
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(existingPackage));

        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(10L);
        when(menuRepository.findByIdIn(anyCollection())).thenReturn(Collections.singletonList(menu));

        Permission permission = mock(Permission.class);
        when(permission.getId()).thenReturn(100L);
        when(permissionRepository.findAllById(dto.getPermissionIds())).thenReturn(Collections.singletonList(permission));

        when(packageRepository.save(any(TenantPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        packageService.grant(TEST_PACKAGE_ID, dto);

        // Then
        ArgumentCaptor<TenantPackage> captor = ArgumentCaptor.forClass(TenantPackage.class);
        verify(packageRepository).save(captor.capture());
        TenantPackage savedPackage = captor.getValue();

        assertEquals(1, savedPackage.getMenus().size());
        assertTrue(savedPackage.getMenus().stream().anyMatch(m -> m.getId().equals(10L)));
        assertEquals(1, savedPackage.getPermissions().size());
        assertTrue(savedPackage.getPermissions().stream().anyMatch(p -> p.getId().equals(100L)));

        String[] expectedRedisKeys = {
            Const.CacheKey.TENANT_PACKAGE_PERMS + ":menus:" + TEST_PACKAGE_ID,
            Const.CacheKey.TENANT_PACKAGE_PERMS + ":permissions:" + TEST_PACKAGE_ID
        };
        verify(redisCommands).del(expectedRedisKeys);
    }

    @Test
    void grant_PackageNotFound_ShouldThrowBizException() {
        // Given
        TenantPackageGrantDTO dto = new TenantPackageGrantDTO();
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> packageService.grant(TEST_PACKAGE_ID, dto));
        assertEquals("套餐不存在", exception.getMessage());
        
        verify(packageRepository, never()).save(any());
        verify(redisCommands, never()).del(any(String[].class));
    }

    @Test
    void grant_WithNullMenuIdsAndPermissionIds_ShouldClearExisting() {
        // Given
        TenantPackageGrantDTO dto = new TenantPackageGrantDTO();
        dto.setMenuIds(null);
        dto.setPermissionIds(null);

        TenantPackage existingPackage = TenantPackage.create(TEST_PACKAGE_NAME, "");
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(existingPackage));
        when(packageRepository.save(any(TenantPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        packageService.grant(TEST_PACKAGE_ID, dto);

        // Then
        ArgumentCaptor<TenantPackage> captor = ArgumentCaptor.forClass(TenantPackage.class);
        verify(packageRepository).save(captor.capture());
        TenantPackage savedPackage = captor.getValue();

        // The collections should be empty (either null or empty set)
        Set<Menu> menus = savedPackage.getMenus();
        Set<Permission> permissions = savedPackage.getPermissions();
        assertTrue(menus == null || menus.isEmpty());
        assertTrue(permissions == null || permissions.isEmpty());
    }

    @Test
    void updateTenantState_Enable_Success() {
        // Given
        TenantPackage existingPackage = TenantPackage.create(TEST_PACKAGE_NAME, "");
        existingPackage.disable();
        
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(existingPackage));
        when(packageRepository.save(any(TenantPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        TenantPackageDTO expectedDto = new TenantPackageDTO();
        expectedDto.setState(Const.TenantPackageState.NORMAL);
        when(packageMapper.toDto(any(TenantPackage.class))).thenReturn(expectedDto);

        // When
        TenantPackageDTO result = packageService.updateTenantState(TEST_PACKAGE_ID, Const.TenantPackageState.NORMAL);

        // Then
        assertEquals(Const.TenantPackageState.NORMAL, result.getState());
        assertEquals(Const.TenantPackageState.NORMAL, existingPackage.getState());
        
        verify(packageRepository).save(existingPackage);
    }

    @Test
    void updateTenantState_Disable_Success() {
        // Given
        TenantPackage existingPackage = TenantPackage.create(TEST_PACKAGE_NAME, "");
        existingPackage.enable();
        
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(existingPackage));
        when(packageRepository.save(any(TenantPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        TenantPackageDTO expectedDto = new TenantPackageDTO();
        expectedDto.setState(Const.TenantPackageState.DISABLED);
        when(packageMapper.toDto(any(TenantPackage.class))).thenReturn(expectedDto);

        // When
        TenantPackageDTO result = packageService.updateTenantState(TEST_PACKAGE_ID, Const.TenantPackageState.DISABLED);

        // Then
        assertEquals(Const.TenantPackageState.DISABLED, result.getState());
        assertEquals(Const.TenantPackageState.DISABLED, existingPackage.getState());
        
        verify(packageRepository).save(existingPackage);
    }

    @Test
    void updateTenantState_PackageNotFound_ShouldThrowBizException() {
        // Given
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> packageService.updateTenantState(TEST_PACKAGE_ID, Const.TenantPackageState.NORMAL));
        assertEquals("套餐不存在", exception.getMessage());
        
        verify(packageRepository, never()).save(any());
    }

    @Test
    void deleteByVerify_Success() {
        // Given
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(mock(TenantPackage.class)));
        when(tenantRepository.countByPackageId(TEST_PACKAGE_ID)).thenReturn(0L);

        // When
        packageService.deleteByVerify(TEST_PACKAGE_ID);

        // Then
        verify(packageRepository).deleteById(TEST_PACKAGE_ID);
    }

    @Test
    void deleteByVerify_PackageNotFound_ShouldThrowBizException() {
        // Given
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> packageService.deleteByVerify(TEST_PACKAGE_ID));
        assertEquals("套餐不存在", exception.getMessage());
        
        verify(packageRepository, never()).deleteById(any());
        verify(tenantRepository, never()).countByPackageId(any());
    }

    @Test
    void deleteByVerify_PackageInUse_ShouldThrowBizException() {
        // Given
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(mock(TenantPackage.class)));
        when(tenantRepository.countByPackageId(TEST_PACKAGE_ID)).thenReturn(1L);

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> packageService.deleteByVerify(TEST_PACKAGE_ID));
        assertEquals("套餐正在使用中，不允许删除", exception.getMessage());
        
        verify(packageRepository, never()).deleteById(any());
    }

    @Test
    void findOptions_WithName_Success() {
        // Given
        String searchName = "test";
        TenantPackage entity1 = TenantPackage.create("Test Package 1", "");
        entity1.enable();
        TenantPackage entity2 = TenantPackage.create("Test Package 2", "");
        entity2.enable();
        
        List<TenantPackage> packages = Arrays.asList(entity1, entity2);
        when(packageRepository.findByNameContainsIgnoreCaseAndState(searchName, Const.TenantPackageState.NORMAL))
            .thenReturn(packages);
        
        TenantPackageDTO dto1 = new TenantPackageDTO();
        dto1.setId(1L);
        dto1.setName("Test Package 1");
        TenantPackageDTO dto2 = new TenantPackageDTO();
        dto2.setId(2L);
        dto2.setName("Test Package 2");
        
        when(packageMapper.toDto(entity1)).thenReturn(dto1);
        when(packageMapper.toDto(entity2)).thenReturn(dto2);

        // When
        List<TenantPackageOptionDTO> result = packageService.findOptions(searchName);

        // Then
        assertEquals(2, result.size());
        assertEquals("Test Package 1", result.get(0).getName());
        assertEquals("Test Package 2", result.get(1).getName());
        
        verify(packageRepository).findByNameContainsIgnoreCaseAndState(searchName, Const.TenantPackageState.NORMAL);
    }

    @Test
    void findOptions_WithoutName_Success() {
        // Given
        TenantPackage entity = TenantPackage.create("Package 1", "");
        entity.enable();
        
        List<TenantPackage> packages = Collections.singletonList(entity);
        when(packageRepository.findByState(Const.TenantPackageState.NORMAL)).thenReturn(packages);
        
        TenantPackageDTO dto = new TenantPackageDTO();
        dto.setId(1L);
        dto.setName("Package 1");
        when(packageMapper.toDto(entity)).thenReturn(dto);

        // When
        List<TenantPackageOptionDTO> result = packageService.findOptions(null);

        // Then
        assertEquals(1, result.size());
        assertEquals("Package 1", result.get(0).getName());
        
        verify(packageRepository).findByState(Const.TenantPackageState.NORMAL);
    }

    @Test
    void getMenuIdsByPackage_CacheHit_Success() throws IOException {
        // Given
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":menus:" + TEST_PACKAGE_ID;
        String cachedValue = "[1,2,3]";
        Set<Long> expectedIds = Set.of(1L, 2L, 3L);
        
        when(redisCommands.get(cacheKey)).thenReturn(cachedValue);
        when(objectMapper.readValue(eq(cachedValue), any(TypeReference.class))).thenReturn(expectedIds);

        // When
        Set<Long> result = packageService.getMenuIdsByPackage(TEST_PACKAGE_ID);

        // Then
        assertEquals(expectedIds, result);
        verify(redisCommands).get(cacheKey);
        verify(objectMapper).readValue(eq(cachedValue), any(TypeReference.class));
        verify(packageRepository, never()).findById(any());
        verify(redisCommands, never()).set(anyString(), anyString());
    }

    @Test
    void getMenuIdsByPackage_PackageNotFound_ShouldReturnEmpty() {
        // Given
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":menus:" + TEST_PACKAGE_ID;
        when(redisCommands.get(cacheKey)).thenReturn(null);
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.empty());

        // When
        Set<Long> result = packageService.getMenuIdsByPackage(TEST_PACKAGE_ID);

        // Then
        assertTrue(result.isEmpty());
        verify(redisCommands).get(cacheKey);
        verify(packageRepository).findById(TEST_PACKAGE_ID);
        verify(redisCommands, never()).set(anyString(), anyString());
    }

    @Test
    void getPermissionIdsByPackage_CacheHit_Success() throws IOException {
        // Given
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":permissions:" + TEST_PACKAGE_ID;
        String cachedValue = "[100,200]";
        Set<Long> expectedIds = Set.of(100L, 200L);
        
        when(redisCommands.get(cacheKey)).thenReturn(cachedValue);
        when(objectMapper.readValue(eq(cachedValue), any(TypeReference.class))).thenReturn(expectedIds);

        // When
        Set<Long> result = packageService.getPermissionIdsByPackage(TEST_PACKAGE_ID);

        // Then
        assertEquals(expectedIds, result);
        verify(redisCommands).get(cacheKey);
        verify(objectMapper).readValue(eq(cachedValue), any(TypeReference.class));
        verify(packageRepository, never()).findById(any());
        verify(redisCommands, never()).set(anyString(), anyString());
    }

    @Test
    void getPermissionIdsByPackage_PackageNotFound_ShouldReturnEmpty() {
        // Given
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":permissions:" + TEST_PACKAGE_ID;
        when(redisCommands.get(cacheKey)).thenReturn(null);
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.empty());

        // When
        Set<Long> result = packageService.getPermissionIdsByPackage(TEST_PACKAGE_ID);

        // Then
        assertTrue(result.isEmpty());
        verify(redisCommands).get(cacheKey);
        verify(packageRepository).findById(TEST_PACKAGE_ID);
        verify(redisCommands, never()).set(anyString(), anyString());
    }

    @Test
    void getById_Success() {
        // Given
        TenantPackage entity = TenantPackage.create(TEST_PACKAGE_NAME, TEST_DESCRIPTION);
        
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(entity));
        
        TenantPackageDTO expectedDto = new TenantPackageDTO();
        expectedDto.setId(TEST_PACKAGE_ID);
        expectedDto.setName(TEST_PACKAGE_NAME);
        expectedDto.setMenus(Collections.emptySet());
        expectedDto.setPermissions(Collections.emptySet());
        when(packageMapper.toDto(entity)).thenReturn(expectedDto);

        // When
        TenantPackageDTO result = packageService.getById(TEST_PACKAGE_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_PACKAGE_NAME, result.getName());
        assertTrue(result.getMenus().isEmpty());
        assertTrue(result.getPermissions().isEmpty());
        
        verify(packageRepository).findById(TEST_PACKAGE_ID);
        verify(packageMapper).toDto(entity);
    }

    @Test
    void getById_PackageNotFound_ShouldThrowBizException() {
        // Given
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.empty());

        // When & Then
        BizException exception = assertThrows(BizException.class, 
            () -> packageService.getById(TEST_PACKAGE_ID));
        assertEquals("套餐不存在", exception.getMessage());
        
        verify(packageMapper, never()).toDto(any());
    }

    @Test
    void getMenuIdsByPackage_PackageWithNullMenus_ShouldReturnEmpty() {
        // Given
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":menus:" + TEST_PACKAGE_ID;
        when(redisCommands.get(cacheKey)).thenReturn(null);
        
        TenantPackage tenantPackage = TenantPackage.create(TEST_PACKAGE_NAME, "");
        // Don't set menus, keep it null
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(tenantPackage));

        // When
        Set<Long> result = packageService.getMenuIdsByPackage(TEST_PACKAGE_ID);

        // Then
        assertTrue(result.isEmpty());
        verify(redisCommands).get(cacheKey);
        verify(packageRepository).findById(TEST_PACKAGE_ID);
    }

    @Test
    void getMenuIdsByPackage_PackageWithNullMenusAndCacheError_ShouldFallbackToDatabase() throws Exception {
        // Given
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":menus:" + TEST_PACKAGE_ID;
        String cachedValue = "invalid_json";
        
        when(redisCommands.get(cacheKey)).thenReturn(cachedValue);
        doThrow(new JsonProcessingException("Invalid JSON") {})
            .when(objectMapper).readValue(eq(cachedValue), any(TypeReference.class));
        
        TenantPackage tenantPackage = TenantPackage.create(TEST_PACKAGE_NAME, "");
        // Don't set menus, keep it null
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(tenantPackage));

        // When
        Set<Long> result = packageService.getMenuIdsByPackage(TEST_PACKAGE_ID);

        // Then
        assertTrue(result.isEmpty());
        verify(redisCommands).get(cacheKey);
        verify(objectMapper).readValue(eq(cachedValue), any(TypeReference.class));
        verify(packageRepository).findById(TEST_PACKAGE_ID);
    }

    @Test
    void getPermissionIdsByPackage_PackageWithNullPermissions_ShouldReturnEmpty() {
        // Given
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":permissions:" + TEST_PACKAGE_ID;
        when(redisCommands.get(cacheKey)).thenReturn(null);
        
        TenantPackage tenantPackage = TenantPackage.create(TEST_PACKAGE_NAME, "");
        // Don't set permissions, keep it null
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(tenantPackage));

        // When
        Set<Long> result = packageService.getPermissionIdsByPackage(TEST_PACKAGE_ID);

        // Then
        assertTrue(result.isEmpty());
        verify(redisCommands).get(cacheKey);
        verify(packageRepository).findById(TEST_PACKAGE_ID);
    }

    @Test
    void getPermissionIdsByPackage_PackageWithNullPermissionsAndCacheError_ShouldFallbackToDatabase() throws Exception {
        // Given
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":permissions:" + TEST_PACKAGE_ID;
        String cachedValue = "invalid_json";
        
        when(redisCommands.get(cacheKey)).thenReturn(cachedValue);
        doThrow(new JsonProcessingException("Invalid JSON") {})
            .when(objectMapper).readValue(eq(cachedValue), any(TypeReference.class));
        
        TenantPackage tenantPackage = TenantPackage.create(TEST_PACKAGE_NAME, "");
        // Don't set permissions, keep it null
        when(packageRepository.findById(TEST_PACKAGE_ID)).thenReturn(Optional.of(tenantPackage));

        // When
        Set<Long> result = packageService.getPermissionIdsByPackage(TEST_PACKAGE_ID);

        // Then
        assertTrue(result.isEmpty());
        verify(redisCommands).get(cacheKey);
        verify(objectMapper).readValue(eq(cachedValue), any(TypeReference.class));
        verify(packageRepository).findById(TEST_PACKAGE_ID);
    }
}