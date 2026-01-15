package com.mok.application.sys.service;

import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.permission.PermissionDTO;
import com.mok.application.sys.mapper.PermissionMapper;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.model.Permission;
import com.mok.domain.sys.repository.MenuRepository;
import com.mok.domain.sys.repository.PermissionRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionServiceTest {

    private PermissionRepository permissionRepository;
    private MenuRepository menuRepository;
    private PermissionMapper permissionMapper;
    private RedisCommands<String, String> redisCommands;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionRepository = mock(PermissionRepository.class);
        menuRepository = mock(MenuRepository.class);
        permissionMapper = mock(PermissionMapper.class);
        redisCommands = mock(RedisCommands.class);
        permissionService = new PermissionService(permissionRepository, menuRepository, permissionMapper, redisCommands);
    }

    @Test
    void createPermission_Success() {
        PermissionDTO dto = new PermissionDTO();
        dto.setName("Test Perm");
        dto.setCode("test:perm");
        dto.setMenuId(1L);

        Menu menu = mock(Menu.class);
        when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));

        permissionService.createPermission(dto);

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        Permission savedPermission = captor.getValue();

        assertEquals("Test Perm", savedPermission.getName());
        assertEquals("test:perm", savedPermission.getCode());
        assertSame(menu, savedPermission.getMenu());
    }

    @Test
    void updatePermission_Success() {
        Long permId = 1L;
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permId);
        dto.setName("Updated Perm");

        Permission existingPermission = Permission.create("Old Perm", "old:perm", null, null, null, null);
        when(permissionRepository.findById(permId)).thenReturn(Optional.of(existingPermission));

        permissionService.updatePermission(dto);

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        Permission savedPermission = captor.getValue();

        assertSame(existingPermission, savedPermission);
        assertEquals("Updated Perm", savedPermission.getName());
    }
    
    @Test
    void updatePermission_NotFound_ShouldThrowNotFoundException() {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(99L);
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> permissionService.updatePermission(dto));
    }

    @Test
    void deleteById_Success() {
        Long permId = 1L;
        List<Long> associatedRoleIds = List.of(10L, 20L);

        when(permissionRepository.findRoleIdsByPermissionId(permId)).thenReturn(associatedRoleIds);

        permissionService.deleteById(permId);

        verify(permissionRepository).deleteRolePermissionsByPermissionId(permId);
        verify(permissionRepository).deleteById(permId);

        String[] expectedRedisKeys = {
            Const.CacheKey.ROLE_PERMS + ":10",
            Const.CacheKey.ROLE_PERMS + ":20"
        };
        verify(redisCommands).del(expectedRedisKeys);
    }
    
    @Test
    void deleteById_WithNoAssociatedRoles() {
        Long permId = 1L;
        when(permissionRepository.findRoleIdsByPermissionId(permId)).thenReturn(Collections.emptyList());

        permissionService.deleteById(permId);

        verify(permissionRepository).deleteById(permId);
        verify(redisCommands, never()).del(any(String.class));
    }
}
