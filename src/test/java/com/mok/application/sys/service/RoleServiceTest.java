package com.mok.application.sys.service;

import com.mok.application.exception.BizException;
import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.role.RoleGrantDTO;
import com.mok.application.sys.dto.role.RoleSaveDTO;
import com.mok.application.sys.mapper.MenuMapper;
import com.mok.application.sys.mapper.PermissionMapper;
import com.mok.application.sys.mapper.RoleMapper;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.model.Permission;
import com.mok.domain.sys.model.Role;
import com.mok.domain.sys.repository.MenuRepository;
import com.mok.domain.sys.repository.PermissionRepository;
import com.mok.domain.sys.repository.RoleRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleServiceTest {

    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private MenuRepository menuRepository;
    private RoleMapper roleMapper;
    private PermissionMapper permissionMapper;
    private MenuMapper menuMapper;
    private RedisCommands<String, String> redisCommands;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        menuRepository = mock(MenuRepository.class);
        roleMapper = mock(RoleMapper.class);
        permissionMapper = mock(PermissionMapper.class);
        menuMapper = mock(MenuMapper.class);
        redisCommands = mock(RedisCommands.class);
        roleService = new RoleService(roleRepository, permissionRepository, menuRepository, roleMapper, permissionMapper, menuMapper, redisCommands);
    }

    @Test
    void createRole_Success() {
        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setName("Test Role");
        dto.setCode("test_role");

        roleService.createRole(dto);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        Role savedRole = captor.getValue();

        assertEquals("Test Role", savedRole.getName());
        assertEquals("test_role", savedRole.getCode());
    }

    @Test
    void updateRole_Success() {
        Long roleId = 1L;
        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setId(roleId);
        dto.setName("Updated Role");

        Role existingRole = Role.create("Old Role", "old_code", null, 1);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));

        roleService.updateRole(dto);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        Role savedRole = captor.getValue();

        assertSame(existingRole, savedRole);
        assertEquals("Updated Role", savedRole.getName());
    }

    @Test
    void deleteRoleBeforeValidation_Success() {
        Long roleId = 1L;
        when(roleRepository.existsByRolesId(roleId)).thenReturn(false);

        roleService.deleteRoleBeforeValidation(roleId);

        verify(roleRepository).deleteById(roleId);
    }

    @Test
    void deleteRoleBeforeValidation_WithAssociatedUsers_ShouldThrowBizException() {
        Long roleId = 1L;
        when(roleRepository.existsByRolesId(roleId)).thenReturn(true);

        Exception exception = assertThrows(BizException.class, () -> roleService.deleteRoleBeforeValidation(roleId));
        assertEquals("该角色下存在用户，请先删除用户关联该角色", exception.getMessage());
    }

    @Test
    void grant_Success() {
        Long roleId = 1L;
        RoleGrantDTO dto = new RoleGrantDTO();
        dto.setMenuIds(Set.of(10L));
        dto.setPermissionIds(Set.of(100L));

        Role existingRole = Role.create("Test Role", "test_role", null, 1);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));

        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(10L);
        when(menuRepository.findByIdIn(new ArrayList<>(dto.getMenuIds()))).thenReturn(Collections.singletonList(menu));

        Permission permission = mock(Permission.class);
        when(permission.getId()).thenReturn(100L);
        when(permissionRepository.findAllById(dto.getPermissionIds())).thenReturn(Collections.singletonList(permission));

        roleService.grant(roleId, dto);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        Role savedRole = captor.getValue();

        assertEquals(1, savedRole.getMenus().size());
        assertTrue(savedRole.getMenus().stream().anyMatch(m -> m.getId().equals(10L)));
        assertEquals(1, savedRole.getPermissions().size());
        assertTrue(savedRole.getPermissions().stream().anyMatch(p -> p.getId().equals(100L)));

        verify(redisCommands).del(Const.CacheKey.ROLE_PERMS + ":" + roleId);
    }
    
    @Test
    void grant_RoleNotFound_ShouldThrowNotFoundException() {
        Long roleId = 99L;
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> roleService.grant(roleId, new RoleGrantDTO()));
    }
}
