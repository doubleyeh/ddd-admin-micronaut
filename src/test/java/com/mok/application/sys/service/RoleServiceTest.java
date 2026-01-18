package com.mok.application.sys.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mok.application.exception.BizException;
import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.dto.permission.PermissionDTO;
import com.mok.application.sys.dto.role.*;
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
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private RedisCommands<String, String> redisCommands;

    private RoleService roleService;
    private Role testRole;
    private final Long TEST_ROLE_ID = 1L;
    private final String TEST_ROLE_NAME = "ADMIN";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        roleService = new RoleService(
            roleRepository,
            permissionRepository,
            menuRepository,
            roleMapper,
            permissionMapper,
            menuMapper,
            redisCommands
        );

        testRole = mock(Role.class);
        when(testRole.getId()).thenReturn(TEST_ROLE_ID);
        when(testRole.getName()).thenReturn(TEST_ROLE_NAME);
        String TEST_ROLE_CODE = "admin_role";
        when(testRole.getCode()).thenReturn(TEST_ROLE_CODE);
        String TEST_DESCRIPTION = "Test admin role";
        when(testRole.getDescription()).thenReturn(TEST_DESCRIPTION);
        when(testRole.getState()).thenReturn(Const.RoleState.NORMAL);
        when(testRole.getMenus()).thenReturn(new HashSet<>());
        when(testRole.getPermissions()).thenReturn(new HashSet<>());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPage_WithValidQuery_ReturnsRolePage() {
        RoleQuery query = new RoleQuery();
        query.setName(TEST_ROLE_NAME);
        Pageable pageable = mock(Pageable.class);

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(TEST_ROLE_ID);
        roleDTO.setName(TEST_ROLE_NAME);

        Page<RoleDTO> rolePage = Page.of(List.of(roleDTO), pageable, 1L);

        when(
            roleRepository.findRolePage(
                any(PredicateSpecification.class),
                eq(pageable)
            )
        ).thenReturn(rolePage);

        Page<RoleDTO> result = roleService.findPage(query, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_ROLE_NAME, result.getContent().getFirst().getName());
        verify(roleRepository).findRolePage(
            any(PredicateSpecification.class),
            eq(pageable)
        );
    }

    @Test
    void getById_WithValidId_ReturnsRoleDTO() {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(TEST_ROLE_ID);
        roleDTO.setName(TEST_ROLE_NAME);
        roleDTO.setMenus(Set.of());
        roleDTO.setPermissions(Set.of());

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(roleMapper.toDto(testRole)).thenReturn(roleDTO);

        RoleDTO result = roleService.getById(TEST_ROLE_ID);

        assertNotNull(result);
        assertEquals(TEST_ROLE_ID, result.getId());
        assertEquals(TEST_ROLE_NAME, result.getName());
        verify(roleRepository).findById(TEST_ROLE_ID);
        verify(roleMapper).toDto(testRole);
    }

    @Test
    void getById_WithNonExistingId_ThrowsNotFoundException() {
        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.empty()
        );

        assertThrows(NotFoundException.class, () -> {
            roleService.getById(TEST_ROLE_ID);
        });
    }

    @Test
    void createRole_WithValidData_ReturnsCreatedRoleDTO() {
        RoleSaveDTO saveDTO = new RoleSaveDTO();
        saveDTO.setName("NEW_ROLE");
        saveDTO.setCode("new_role");
        saveDTO.setDescription("New test role");
        saveDTO.setSort(1);

        Role newRole = mock(Role.class);
        when(roleRepository.save(any(Role.class))).thenReturn(newRole);

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(TEST_ROLE_ID);
        roleDTO.setName("NEW_ROLE");

        when(roleMapper.toDto(newRole)).thenReturn(roleDTO);

        RoleDTO result = roleService.createRole(saveDTO);

        assertNotNull(result);
        assertEquals("NEW_ROLE", result.getName());
        verify(roleRepository).save(any(Role.class));
        verify(roleMapper).toDto(newRole);
    }

    @Test
    void updateRole_WithValidData_ReturnsUpdatedRoleDTO() {
        RoleSaveDTO saveDTO = new RoleSaveDTO();
        saveDTO.setId(TEST_ROLE_ID);
        saveDTO.setName("UPDATED_ROLE");
        saveDTO.setCode("updated_role");
        saveDTO.setDescription("Updated description");
        saveDTO.setSort(1);

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(TEST_ROLE_ID);
        roleDTO.setName("UPDATED_ROLE");

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(roleRepository.save(testRole)).thenReturn(testRole);
        when(roleMapper.toDto(testRole)).thenReturn(roleDTO);

        RoleDTO result = roleService.updateRole(saveDTO);

        assertNotNull(result);
        assertEquals("UPDATED_ROLE", result.getName());
        verify(roleRepository).findById(TEST_ROLE_ID);
        verify(roleRepository).save(testRole);
        verify(roleMapper).toDto(testRole);
    }

    @Test
    void updateRole_WithNonExistingId_ThrowsNotFoundException() {
        RoleSaveDTO saveDTO = new RoleSaveDTO();
        saveDTO.setId(TEST_ROLE_ID);
        saveDTO.setName("UPDATED_ROLE");

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.empty()
        );

        assertThrows(NotFoundException.class, () -> {
            roleService.updateRole(saveDTO);
        });
    }

    @Test
    void updateState_WithValidNormalState_EnablesRole() {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(TEST_ROLE_ID);

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(roleRepository.save(testRole)).thenReturn(testRole);
        when(roleMapper.toDto(testRole)).thenReturn(roleDTO);

        RoleDTO result = roleService.updateState(
            TEST_ROLE_ID,
            Const.RoleState.NORMAL
        );

        assertNotNull(result);
        verify(testRole).enable();
        verify(roleRepository).save(testRole);
        verify(roleMapper).toDto(testRole);
    }

    @Test
    void updateState_WithValidDisabledState_DisablesRole() {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(TEST_ROLE_ID);

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(roleRepository.save(testRole)).thenReturn(testRole);
        when(roleMapper.toDto(testRole)).thenReturn(roleDTO);

        RoleDTO result = roleService.updateState(
            TEST_ROLE_ID,
            Const.RoleState.DISABLED
        );

        assertNotNull(result);
        verify(testRole).disable();
        verify(roleRepository).save(testRole);
        verify(roleMapper).toDto(testRole);
    }

    @Test
    void updateState_WithNonExistingId_ThrowsNotFoundException() {
        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.empty()
        );

        assertThrows(NotFoundException.class, () -> {
            roleService.updateState(TEST_ROLE_ID, Const.RoleState.NORMAL);
        });
    }

    @Test
    void updateState_WithInvalidState_DoesNotChangeRoleState() {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(TEST_ROLE_ID);

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(roleRepository.save(testRole)).thenReturn(testRole);
        when(roleMapper.toDto(testRole)).thenReturn(roleDTO);

        // 使用一个既不是NORMAL也不是DISABLED的状态值
        Integer invalidState = 999;
        RoleDTO result = roleService.updateState(TEST_ROLE_ID, invalidState);

        assertNotNull(result);
        // 验证既没有调用enable()也没有调用disable()
        verify(testRole, never()).enable();
        verify(testRole, never()).disable();
        verify(roleRepository).save(testRole);
        verify(roleMapper).toDto(testRole);
    }

    @Test
    void deleteRoleBeforeValidation_WithValidId_DeletesRoleSuccessfully() {
        when(roleRepository.existsByRolesId(TEST_ROLE_ID)).thenReturn(false);

        roleService.deleteRoleBeforeValidation(TEST_ROLE_ID);

        verify(roleRepository).deleteById(TEST_ROLE_ID);
    }

    @Test
    void deleteRoleBeforeValidation_WithAssignedUsers_ThrowsBizException() {
        when(roleRepository.existsByRolesId(TEST_ROLE_ID)).thenReturn(true);

        BizException exception = assertThrows(BizException.class, () -> {
            roleService.deleteRoleBeforeValidation(TEST_ROLE_ID);
        });

        assertEquals(
            "该角色下存在用户，请先删除用户关联该角色",
            exception.getMessage()
        );
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void grant_WithMenuAndPermissionIds_GrantsSuccessfully() {
        RoleGrantDTO grantDTO = new RoleGrantDTO();
        grantDTO.setMenuIds(Set.of(1L, 2L));
        grantDTO.setPermissionIds(Set.of(1L, 2L));

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(menuRepository.findByIdIn(Set.of(1L, 2L))).thenReturn(List.of());
        when(permissionRepository.findAllById(Set.of(1L, 2L))).thenReturn(
            List.of()
        );

        roleService.grant(TEST_ROLE_ID, grantDTO);

        verify(testRole).changeMenus(any(Set.class));
        verify(testRole).changePermissions(any(Set.class));
        verify(roleRepository).save(testRole);
        verify(redisCommands).del(
            Const.CacheKey.ROLE_PERMS + ":" + TEST_ROLE_ID
        );
    }

    @Test
    void grant_WithNonExistingRole_ThrowsNotFoundException() {
        RoleGrantDTO grantDTO = new RoleGrantDTO();
        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.empty()
        );

        assertThrows(NotFoundException.class, () -> {
            roleService.grant(TEST_ROLE_ID, grantDTO);
        });
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void grant_WithOnlyMenuIds_GrantsSuccessfully() {
        RoleGrantDTO grantDTO = new RoleGrantDTO();
        grantDTO.setMenuIds(Set.of(1L, 2L));
        grantDTO.setPermissionIds(null);

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(menuRepository.findByIdIn(Set.of(1L, 2L))).thenReturn(List.of());

        roleService.grant(TEST_ROLE_ID, grantDTO);

        verify(testRole).changeMenus(any(Set.class));
        verify(testRole, never()).changePermissions(any(Set.class));
        verify(roleRepository).save(testRole);
        verify(redisCommands).del(
            Const.CacheKey.ROLE_PERMS + ":" + TEST_ROLE_ID
        );
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void grant_WithOnlyPermissionIds_GrantsSuccessfully() {
        RoleGrantDTO grantDTO = new RoleGrantDTO();
        grantDTO.setMenuIds(null);
        grantDTO.setPermissionIds(Set.of(1L, 2L));

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(permissionRepository.findAllById(Set.of(1L, 2L))).thenReturn(
            List.of()
        );

        roleService.grant(TEST_ROLE_ID, grantDTO);

        verify(testRole, never()).changeMenus(any(Set.class));
        verify(testRole).changePermissions(any(Set.class));
        verify(roleRepository).save(testRole);
        verify(redisCommands).del(
            Const.CacheKey.ROLE_PERMS + ":" + TEST_ROLE_ID
        );
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void grant_WithEmptyMenuAndPermissionIds_GrantsSuccessfully() {
        RoleGrantDTO grantDTO = new RoleGrantDTO();
        grantDTO.setMenuIds(Set.of());
        grantDTO.setPermissionIds(Set.of());

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(menuRepository.findByIdIn(Set.of())).thenReturn(List.of());
        when(permissionRepository.findAllById(Set.of())).thenReturn(List.of());

        roleService.grant(TEST_ROLE_ID, grantDTO);

        verify(testRole).changeMenus(any(Set.class));
        verify(testRole).changePermissions(any(Set.class));
        verify(roleRepository).save(testRole);
        verify(redisCommands).del(
            Const.CacheKey.ROLE_PERMS + ":" + TEST_ROLE_ID
        );
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void getRoleOptions_WithValidQuery_ReturnsRoleOptionList() {
        RoleQuery query = new RoleQuery();

        RoleOptionDTO optionDTO = new RoleOptionDTO();
        optionDTO.setId(TEST_ROLE_ID);
        optionDTO.setName(TEST_ROLE_NAME);

        when(
            roleRepository.findAll(any(PredicateSpecification.class))
        ).thenReturn(List.of(testRole));
        when(roleMapper.toOptionsDto(testRole)).thenReturn(optionDTO);

        List<RoleOptionDTO> result = roleService.getRoleOptions(query);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_ROLE_ID, result.getFirst().getId());
        assertEquals(TEST_ROLE_NAME, result.getFirst().getName());
        verify(roleRepository).findAll(any(PredicateSpecification.class));
        verify(roleMapper).toOptionsDto(testRole);
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void getRoleOptions_WithDisabledRole_ExcludesDisabledRoles() {
        RoleQuery query = new RoleQuery();
        Role disabledRole = mock(Role.class);
        when(disabledRole.getState()).thenReturn(Const.RoleState.DISABLED);

        when(
            roleRepository.findAll(any(PredicateSpecification.class))
        ).thenReturn(List.of(disabledRole));

        List<RoleOptionDTO> result = roleService.getRoleOptions(query);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(roleRepository).findAll(any(PredicateSpecification.class));
        verify(roleMapper, never()).toOptionsDto(any());
    }

    @Test
    void getMenusByRole_WithValidId_ReturnsMenuDTOSet() {
        Menu menu = mock(Menu.class);
        Set<Menu> menus = Set.of(menu);

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(testRole.getMenus()).thenReturn(menus);

        MenuDTO menuDTO = new MenuDTO();
        when(menuMapper.toDto(menu)).thenReturn(menuDTO);

        Set<MenuDTO> result = roleService.getMenusByRole(TEST_ROLE_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(roleRepository).findById(TEST_ROLE_ID);
        verify(menuMapper).toDto(menu);
    }

    @Test
    void getMenusByRole_WithNonExistingId_ThrowsNotFoundException() {
        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.empty()
        );

        assertThrows(NotFoundException.class, () -> {
            roleService.getMenusByRole(TEST_ROLE_ID);
        });
    }

    @Test
    void getPermissionsByRole_WithValidId_ReturnsPermissionDTOSet() {
        Permission permission = mock(Permission.class);
        Set<Permission> permissions = Set.of(permission);

        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.of(testRole)
        );
        when(testRole.getPermissions()).thenReturn(permissions);

        PermissionDTO permissionDTO = new PermissionDTO();
        when(permissionMapper.toDto(permission)).thenReturn(permissionDTO);

        Set<PermissionDTO> result = roleService.getPermissionsByRole(
            TEST_ROLE_ID
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(roleRepository).findById(TEST_ROLE_ID);
        verify(permissionMapper).toDto(permission);
    }

    @Test
    void getPermissionsByRole_WithNonExistingId_ThrowsNotFoundException() {
        when(roleRepository.findById(TEST_ROLE_ID)).thenReturn(
            Optional.empty()
        );

        assertThrows(NotFoundException.class, () -> {
            roleService.getPermissionsByRole(TEST_ROLE_ID);
        });
    }
}
