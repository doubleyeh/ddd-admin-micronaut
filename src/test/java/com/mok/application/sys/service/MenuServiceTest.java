package com.mok.application.sys.service;

import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.mapper.MenuMapper;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.repository.MenuRepository;
import com.mok.domain.sys.repository.PermissionRepository;
import com.mok.domain.sys.repository.TenantRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MenuServiceTest {

    private MenuRepository menuRepository;
    private PermissionRepository permissionRepository;
    private RedisCommands<String, String> redisCommands;
    private MenuMapper menuMapper;
    private TenantRepository tenantRepository;
    private TenantPackageService tenantPackageService;
    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuRepository = mock(MenuRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        redisCommands = mock(RedisCommands.class);
        menuMapper = mock(MenuMapper.class);
        tenantRepository = mock(TenantRepository.class);
        tenantPackageService = mock(TenantPackageService.class);
        menuService = new MenuService(menuRepository, permissionRepository, redisCommands, menuMapper, tenantRepository, tenantPackageService);
    }

    @Test
    void createMenu_Success_WithParent() {
        MenuDTO dto = new MenuDTO();
        dto.setParentId(1L);
        dto.setName("Child Menu");
        dto.setPath("/child");

        Menu parentMenu = mock(Menu.class);
        when(menuRepository.findById(1L)).thenReturn(Optional.of(parentMenu));

        menuService.createMenu(dto);

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());
        Menu savedMenu = captor.getValue();

        assertEquals("Child Menu", savedMenu.getName());
        assertEquals("/child", savedMenu.getPath());
        assertSame(parentMenu, savedMenu.getParent());
    }

    @Test
    void createMenu_Success_RootMenu() {
        MenuDTO dto = new MenuDTO();
        dto.setName("Root Menu");
        dto.setPath("/root");

        menuService.createMenu(dto);

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());
        Menu savedMenu = captor.getValue();

        assertEquals("Root Menu", savedMenu.getName());
        assertNull(savedMenu.getParent());
    }

    @Test
    void updateMenu_Success() {
        Long menuId = 1L;
        MenuDTO dto = new MenuDTO();
        dto.setId(menuId);
        dto.setName("Updated Menu");
        dto.setPath("/updated");

        Menu existingMenu = Menu.create(null, "Old Menu", "/old", null, null, 1, false);

        when(menuRepository.findById(menuId)).thenReturn(Optional.of(existingMenu));

        menuService.updateMenu(dto);

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());
        Menu savedMenu = captor.getValue();

        assertSame(existingMenu, savedMenu);
        assertEquals("Updated Menu", savedMenu.getName());
        assertEquals("/updated", savedMenu.getPath());
    }

    @Test
    void updateMenu_NotFound_ShouldThrowNotFoundException() {
        MenuDTO dto = new MenuDTO();
        dto.setId(99L);
        when(menuRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> menuService.updateMenu(dto));
    }

    @Test
    void deleteById_Success() {
        Long menuIdToDelete = 1L;
        Long childId = 2L;

        Menu parentMenu = mock(Menu.class);
        when(parentMenu.getId()).thenReturn(menuIdToDelete);
        Menu childMenu = mock(Menu.class);
        when(childMenu.getId()).thenReturn(childId);

        // Mock the recursive structure
        when(menuRepository.findByParentId(menuIdToDelete)).thenReturn(Collections.singletonList(childMenu));
        when(menuRepository.findByParentId(childId)).thenReturn(Collections.emptyList());
        
        List<Long> allIds = new ArrayList<>(List.of(menuIdToDelete, childId));
        List<Long> roleIds = List.of(10L);

        when(menuRepository.findRoleIdsByMenuIds(allIds)).thenReturn(roleIds);

        menuService.deleteById(menuIdToDelete);

        verify(permissionRepository).deleteRolePermissionsByMenuIds(allIds);
        verify(permissionRepository).deleteByMenuIds(allIds);
        verify(menuRepository).deleteRoleMenuByMenuIds(allIds);
        verify(menuRepository).deleteAllById(allIds);
        verify(redisCommands).del(Const.CacheKey.MENU_TREE);
        verify(redisCommands).del(Const.CacheKey.ROLE_PERMS + ":" + 10L);
    }
}
