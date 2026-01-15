package com.mok.application.sys.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mok.application.exception.BizException;
import com.mok.application.sys.dto.tenantPackage.TenantPackageGrantDTO;
import com.mok.application.sys.dto.tenantPackage.TenantPackageSaveDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantPackageServiceTest {

    private TenantPackageRepository packageRepository;
    private MenuRepository menuRepository;
    private PermissionRepository permissionRepository;
    private TenantPackageMapper packageMapper;
    private TenantRepository tenantRepository;
    private RedisCommands<String, String> redisCommands;
    private MenuMapper menuMapper;
    private PermissionMapper permissionMapper;
    private ObjectMapper objectMapper;
    private TenantPackageService packageService;

    @BeforeEach
    void setUp() {
        packageRepository = mock(TenantPackageRepository.class);
        menuRepository = mock(MenuRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        packageMapper = mock(TenantPackageMapper.class);
        tenantRepository = mock(TenantRepository.class);
        redisCommands = mock(RedisCommands.class);
        menuMapper = mock(MenuMapper.class);
        permissionMapper = mock(PermissionMapper.class);
        objectMapper = new ObjectMapper(); // Real object mapper for serialization tests
        packageService = new TenantPackageService(packageRepository, menuRepository, permissionRepository, packageMapper, tenantRepository, redisCommands, menuMapper, permissionMapper, objectMapper);
    }

    @Test
    void createPackage_Success() {
        TenantPackageSaveDTO dto = new TenantPackageSaveDTO();
        dto.setName("New Package");
        dto.setDescription("A new package");

        packageService.createPackage(dto);

        ArgumentCaptor<TenantPackage> captor = ArgumentCaptor.forClass(TenantPackage.class);
        verify(packageRepository).save(captor.capture());
        TenantPackage savedPackage = captor.getValue();

        assertEquals("New Package", savedPackage.getName());
        assertEquals("A new package", savedPackage.getDescription());
    }

    @Test
    void grant_Success() {
        Long packageId = 1L;
        TenantPackageGrantDTO dto = new TenantPackageGrantDTO();
        dto.setMenuIds(Set.of(10L));
        dto.setPermissionIds(Set.of(100L));

        TenantPackage existingPackage = TenantPackage.create("Test Package", "");
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(existingPackage));

        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(10L);
        when(menuRepository.findByIdIn(new ArrayList<>(dto.getMenuIds()))).thenReturn(Collections.singletonList(menu));

        Permission permission = mock(Permission.class);
        when(permission.getId()).thenReturn(100L);
        when(permissionRepository.findAllById(dto.getPermissionIds())).thenReturn(Collections.singletonList(permission));

        packageService.grant(packageId, dto);

        ArgumentCaptor<TenantPackage> captor = ArgumentCaptor.forClass(TenantPackage.class);
        verify(packageRepository).save(captor.capture());
        TenantPackage savedPackage = captor.getValue();

        assertEquals(1, savedPackage.getMenus().size());
        assertTrue(savedPackage.getMenus().stream().anyMatch(m -> m.getId().equals(10L)));
        assertEquals(1, savedPackage.getPermissions().size());
        assertTrue(savedPackage.getPermissions().stream().anyMatch(p -> p.getId().equals(100L)));

        String[] expectedRedisKeys = {
            Const.CacheKey.TENANT_PACKAGE_PERMS + ":menus:" + packageId,
            Const.CacheKey.TENANT_PACKAGE_PERMS + ":permissions:" + packageId
        };
        verify(redisCommands).del(expectedRedisKeys);
    }

    @Test
    void deleteByVerify_Success() {
        Long packageId = 1L;
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(mock(TenantPackage.class)));
        when(tenantRepository.countByPackageId(packageId)).thenReturn(0L);

        packageService.deleteByVerify(packageId);

        verify(packageRepository).deleteById(packageId);
    }

    @Test
    void deleteByVerify_PackageInUse_ShouldThrowBizException() {
        Long packageId = 1L;
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(mock(TenantPackage.class)));
        when(tenantRepository.countByPackageId(packageId)).thenReturn(1L);

        Exception exception = assertThrows(BizException.class, () -> packageService.deleteByVerify(packageId));
        assertEquals("套餐正在使用中，不允许删除", exception.getMessage());
    }
}
