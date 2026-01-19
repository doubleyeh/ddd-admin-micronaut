package com.mok.sys.application.service;

import com.mok.common.application.exception.NotFoundException;
import com.mok.sys.application.dto.permission.PermissionDTO;
import com.mok.sys.application.dto.permission.PermissionQuery;
import com.mok.sys.application.mapper.PermissionMapper;
import com.mok.sys.domain.model.Menu;
import com.mok.sys.domain.model.Permission;
import com.mok.sys.domain.repository.MenuRepository;
import com.mok.sys.domain.repository.PermissionRepository;
import com.mok.common.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private RedisCommands<String, String> redisCommands;

    private PermissionService permissionService;
    private Permission testPermission;
    private final Long TEST_PERMISSION_ID = 1L;
    private final String TEST_PERMISSION_NAME = "TEST_PERMISSION";
    private final String TEST_PERMISSION_CODE = "test:permission";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        permissionService = new PermissionService(
            permissionRepository,
            menuRepository,
            permissionMapper,
            redisCommands
        );

        testPermission = mock(Permission.class);
        when(testPermission.getId()).thenReturn(TEST_PERMISSION_ID);
        when(testPermission.getName()).thenReturn(TEST_PERMISSION_NAME);
        when(testPermission.getCode()).thenReturn(TEST_PERMISSION_CODE);
        when(testPermission.getUrl()).thenReturn("/api/test");
        when(testPermission.getMethod()).thenReturn("GET");
        when(testPermission.getDescription()).thenReturn("Test permission");
        when(testPermission.getMenu()).thenReturn(null);
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

    @Test
    @SuppressWarnings("unchecked")
    void findPage_WithValidQuery_ReturnsPermissionPage() {
        PermissionQuery query = new PermissionQuery();
        query.setMenuId(1L);
        Pageable pageable = mock(Pageable.class);

        PermissionDTO permissionDTO = new PermissionDTO();
        permissionDTO.setId(TEST_PERMISSION_ID);
        permissionDTO.setName(TEST_PERMISSION_NAME);
        permissionDTO.setCode(TEST_PERMISSION_CODE);

        Page<Permission> permissionPage = Page.of(List.of(testPermission), pageable, 1L);
        Page<PermissionDTO> resultPage = Page.of(List.of(permissionDTO), pageable, 1L);

        when(permissionRepository.findAll(any(PredicateSpecification.class), eq(pageable))).thenReturn(permissionPage);
        when(permissionMapper.toDto(testPermission)).thenReturn(permissionDTO);

        Page<PermissionDTO> result = permissionService.findPage(query, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_PERMISSION_NAME, result.getContent().getFirst().getName());
        verify(permissionRepository).findAll(any(PredicateSpecification.class), eq(pageable));
        verify(permissionMapper).toDto(testPermission);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_WithValidQuery_ReturnsPermissionList() {
        PermissionQuery query = new PermissionQuery();
        query.setMenuId(1L);

        PermissionDTO permissionDTO = new PermissionDTO();
        permissionDTO.setId(TEST_PERMISSION_ID);
        permissionDTO.setName(TEST_PERMISSION_NAME);
        permissionDTO.setCode(TEST_PERMISSION_CODE);

        when(permissionRepository.findAll(any(PredicateSpecification.class))).thenReturn(List.of(testPermission));
        when(permissionMapper.toDto(testPermission)).thenReturn(permissionDTO);

        List<PermissionDTO> result = permissionService.findAll(query);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_PERMISSION_NAME, result.getFirst().getName());
        verify(permissionRepository).findAll(any(PredicateSpecification.class));
        verify(permissionMapper).toDto(testPermission);
    }

    @Test
    void getAllPermissionCodes_WithExistingPermissions_ReturnsCodeSet() {
        when(permissionRepository.findAll()).thenReturn(List.of(testPermission));

        Set<String> result = permissionService.getAllPermissionCodes();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(TEST_PERMISSION_CODE));
        verify(permissionRepository).findAll();
    }

    @Test
    void getAllPermissionCodes_WithNoPermissions_ReturnsEmptySet() {
        when(permissionRepository.findAll()).thenReturn(Collections.emptyList());

        Set<String> result = permissionService.getAllPermissionCodes();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(permissionRepository).findAll();
    }

    @Test
    void getPermissionsByRoleIds_WithNullRoleIds_ReturnsEmptySet() {
        Set<String> result = permissionService.getPermissionsByRoleIds(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(permissionRepository, never()).findCodesByRoleId(any());
        verify(redisCommands, never()).lrange(any(), anyInt(), anyInt());
    }

    @Test
    void getPermissionsByRoleIds_WithEmptyRoleIds_ReturnsEmptySet() {
        Set<String> result = permissionService.getPermissionsByRoleIds(Collections.emptySet());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(permissionRepository, never()).findCodesByRoleId(any());
        verify(redisCommands, never()).lrange(any(), anyInt(), anyInt());
    }

    @Test
    void getPermissionsByRoleIds_WithCacheHit_ReturnsCachedPermissions() {
        Set<Long> roleIds = Set.of(1L, 2L);
        List<String> cachedPerms1 = List.of("perm1", "perm2");
        List<String> cachedPerms2 = List.of("perm3");

        when(redisCommands.lrange(Const.CacheKey.ROLE_PERMS + ":1", 0, -1)).thenReturn(cachedPerms1);
        when(redisCommands.lrange(Const.CacheKey.ROLE_PERMS + ":2", 0, -1)).thenReturn(cachedPerms2);

        Set<String> result = permissionService.getPermissionsByRoleIds(roleIds);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("perm1"));
        assertTrue(result.contains("perm2"));
        assertTrue(result.contains("perm3"));
        verify(redisCommands).lrange(Const.CacheKey.ROLE_PERMS + ":1", 0, -1);
        verify(redisCommands).lrange(Const.CacheKey.ROLE_PERMS + ":2", 0, -1);
        verify(permissionRepository, never()).findCodesByRoleId(any());
    }

    @Test
    void getPermissionsByRoleIds_WithCacheMiss_ReturnsFromDatabaseAndCaches() {
        Set<Long> roleIds = Set.of(1L);
        List<String> dbPerms = List.of("db_perm1", "db_perm2");

        when(redisCommands.lrange(Const.CacheKey.ROLE_PERMS + ":1", 0, -1)).thenReturn(Collections.emptyList());
        when(permissionRepository.findCodesByRoleId(1L)).thenReturn(dbPerms);

        Set<String> result = permissionService.getPermissionsByRoleIds(roleIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("db_perm1"));
        assertTrue(result.contains("db_perm2"));
        verify(redisCommands).lrange(Const.CacheKey.ROLE_PERMS + ":1", 0, -1);
        verify(permissionRepository).findCodesByRoleId(1L);
        verify(redisCommands).rpush(Const.CacheKey.ROLE_PERMS + ":1", "db_perm1", "db_perm2");
    }

    @Test
    void getPermissionsByRoleIds_WithNullCache_ReturnsFromDatabaseAndCaches() {
        Set<Long> roleIds = Set.of(1L);
        List<String> dbPerms = List.of("db_perm1");

        when(redisCommands.lrange(Const.CacheKey.ROLE_PERMS + ":1", 0, -1)).thenReturn(null);
        when(permissionRepository.findCodesByRoleId(1L)).thenReturn(dbPerms);

        Set<String> result = permissionService.getPermissionsByRoleIds(roleIds);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains("db_perm1"));
        verify(redisCommands).lrange(Const.CacheKey.ROLE_PERMS + ":1", 0, -1);
        verify(permissionRepository).findCodesByRoleId(1L);
        verify(redisCommands).rpush(Const.CacheKey.ROLE_PERMS + ":1", "db_perm1");
    }

    @Test
    void getPermissionsByRoleIds_WithEmptyDatabaseResult_ReturnsEmptySet() {
        Set<Long> roleIds = Set.of(1L);

        when(redisCommands.lrange(Const.CacheKey.ROLE_PERMS + ":1", 0, -1)).thenReturn(Collections.emptyList());
        when(permissionRepository.findCodesByRoleId(1L)).thenReturn(Collections.emptyList());

        Set<String> result = permissionService.getPermissionsByRoleIds(roleIds);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(redisCommands).lrange(Const.CacheKey.ROLE_PERMS + ":1", 0, -1);
        verify(permissionRepository).findCodesByRoleId(1L);
        verify(redisCommands, never()).rpush(any(), any(String[].class));
    }

    @Test
    void createPermission_WithNullMenuId_CreatesPermissionWithoutMenu() {
        PermissionDTO dto = new PermissionDTO();
        dto.setName("Test Perm");
        dto.setCode("test:perm");
        dto.setMenuId(null);

        permissionService.createPermission(dto);

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        Permission savedPermission = captor.getValue();

        assertEquals("Test Perm", savedPermission.getName());
        assertEquals("test:perm", savedPermission.getCode());
        assertNull(savedPermission.getMenu());
        verify(menuRepository, never()).findById(any());
    }

    @Test
    void updatePermission_WithNullMenuId_UpdatesPermissionWithoutMenu() {
        Long permId = 1L;
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permId);
        dto.setName("Updated Perm");
        dto.setMenuId(null);

        Permission existingPermission = Permission.create("Old Perm", "old:perm", null, null, null, null);
        when(permissionRepository.findById(permId)).thenReturn(Optional.of(existingPermission));

        permissionService.updatePermission(dto);

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        Permission savedPermission = captor.getValue();

        assertSame(existingPermission, savedPermission);
        assertEquals("Updated Perm", savedPermission.getName());
        assertNull(savedPermission.getMenu());
        verify(menuRepository, never()).findById(any());
    }

    @Test
    void updatePermission_WithValidMenuId_UpdatesPermissionWithMenu() {
        Long permId = 1L;
        Long menuId = 1L;
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permId);
        dto.setName("Updated Perm");
        dto.setMenuId(menuId);

        Menu menu = mock(Menu.class);
        Permission existingPermission = Permission.create("Old Perm", "old:perm", null, null, null, null);
        when(permissionRepository.findById(permId)).thenReturn(Optional.of(existingPermission));
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

        permissionService.updatePermission(dto);

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        Permission savedPermission = captor.getValue();

        assertSame(existingPermission, savedPermission);
        assertEquals("Updated Perm", savedPermission.getName());
        assertSame(menu, savedPermission.getMenu());
        verify(menuRepository).findById(menuId);
    }
}
