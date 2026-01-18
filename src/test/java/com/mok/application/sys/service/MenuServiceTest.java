package com.mok.application.sys.service;

import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.dto.menu.MenuOptionDTO;
import com.mok.application.sys.mapper.MenuMapper;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.model.Permission;
import com.mok.domain.sys.repository.MenuRepository;
import com.mok.domain.sys.repository.PermissionRepository;
import com.mok.domain.sys.repository.TenantRepository;
import com.mok.infrastructure.common.Const;
import com.mok.infrastructure.tenant.TenantContextHolder;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

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
    void findAll_Success() {
        Menu menu = mock(Menu.class);
        when(menuRepository.findAll()).thenReturn(List.of(menu));
        when(menuMapper.toDtoList(any())).thenReturn(List.of(new MenuDTO()));

        List<MenuDTO> result = menuService.findAll();
        assertFalse(result.isEmpty());
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
    void createMenu_ParentNotFound() {
        MenuDTO dto = new MenuDTO();
        dto.setParentId(999L);
        dto.setName("Orphan Menu");
        dto.setPath("/orphan");

        when(menuRepository.findById(999L)).thenReturn(Optional.empty());

        menuService.createMenu(dto);

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());
        Menu savedMenu = captor.getValue();

        assertEquals("Orphan Menu", savedMenu.getName());
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
    void updateMenu_WithParent() {
        Long menuId = 1L;
        Long parentId = 2L;
        MenuDTO dto = new MenuDTO();
        dto.setId(menuId);
        dto.setParentId(parentId);
        dto.setName("Updated");

        Menu existingMenu = mock(Menu.class);
        Menu parentMenu = mock(Menu.class);

        when(menuRepository.findById(menuId)).thenReturn(Optional.of(existingMenu));
        when(menuRepository.findById(parentId)).thenReturn(Optional.of(parentMenu));
        when(menuRepository.save(any())).thenReturn(existingMenu);
        when(menuMapper.toDto(any())).thenReturn(dto);

        menuService.updateMenu(dto);

        verify(existingMenu).updateInfo(eq(parentMenu), eq("Updated"), any(), any(), any(), any(), any());
    }

    @Test
    void updateMenu_ParentNotFound() {
        Long menuId = 1L;
        Long parentId = 999L;
        MenuDTO dto = new MenuDTO();
        dto.setId(menuId);
        dto.setParentId(parentId);
        dto.setName("Updated");

        Menu existingMenu = mock(Menu.class);

        when(menuRepository.findById(menuId)).thenReturn(Optional.of(existingMenu));
        when(menuRepository.findById(parentId)).thenReturn(Optional.empty());
        when(menuRepository.save(any())).thenReturn(existingMenu);
        when(menuMapper.toDto(any())).thenReturn(dto);

        menuService.updateMenu(dto);

        verify(existingMenu).updateInfo(isNull(), eq("Updated"), any(), any(), any(), any(), any());
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

    @Test
    void deleteById_NoRoles() {
        Long menuId = 1L;
        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(menuId);
        when(menuRepository.findByParentId(menuId)).thenReturn(Collections.emptyList());
        when(menuRepository.findRoleIdsByMenuIds(anyList())).thenReturn(Collections.emptyList());

        menuService.deleteById(menuId);

        verify(redisCommands, never()).del(startsWith(Const.CacheKey.ROLE_PERMS));
        verify(redisCommands).del(Const.CacheKey.MENU_TREE);
    }

    @Test
    void changePermissions_Success() {
        Long menuId = 1L;
        Set<Long> permissionIds = new HashSet<>(List.of(10L, 11L));

        Menu menu = mock(Menu.class);
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

        Permission p1 = mock(Permission.class);
        Permission p2 = mock(Permission.class);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(List.of(p1, p2));

        menuService.changePermissions(menuId, permissionIds);

        verify(menu).changePermissions(anySet());
        verify(menuRepository).save(menu);
    }

    @Test
    void changePermissions_Clear() {
        Long menuId = 1L;
        Menu menu = mock(Menu.class);
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

        menuService.changePermissions(menuId, null);

        verify(menu).changePermissions(argThat(Set::isEmpty));
    }

    @Test
    void changePermissions_WithEmptySet() {
        Long menuId = 1L;
        Menu menu = mock(Menu.class);
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));

        menuService.changePermissions(menuId, Collections.emptySet());

        verify(menu).changePermissions(argThat(Set::isEmpty));
        verify(permissionRepository, never()).findAllById(any());
    }

    @Test
    void buildMenuTree_Success() {
        MenuDTO root = new MenuDTO();
        root.setId(1L);
        root.setName("Root");
        root.setParentId(null);
        root.setPath("/root");

        MenuDTO child = new MenuDTO();
        child.setId(2L);
        child.setName("Child");
        child.setParentId(1L);
        child.setPath("/child");

        List<MenuDTO> flatList = List.of(root, child);

        List<MenuDTO> tree = menuService.buildMenuTree(flatList);

        assertEquals(1, tree.size());
        assertEquals("Root", tree.getFirst().getName());
        assertNotNull(tree.getFirst().getChildren());
        assertEquals(1, tree.getFirst().getChildren().size());
        assertEquals("Child", tree.getFirst().getChildren().getFirst().getName());
    }

    @Test
    void buildMenuTree_ParentIdZero() {
        MenuDTO root = new MenuDTO();
        root.setId(1L);
        root.setName("Root");
        root.setParentId(0L);
        root.setPath("/root");

        List<MenuDTO> flatList = List.of(root);

        List<MenuDTO> tree = menuService.buildMenuTree(flatList);

        assertEquals(1, tree.size());
        assertEquals("Root", tree.getFirst().getName());
    }

    @Test
    void buildMenuTree_MultipleChildren() {
        MenuDTO root = new MenuDTO();
        root.setId(1L);
        root.setName("Root");
        root.setPath("/root");

        MenuDTO child1 = new MenuDTO();
        child1.setId(2L);
        child1.setParentId(1L);
        child1.setName("Child1");
        child1.setPath("/child1");

        MenuDTO child2 = new MenuDTO();
        child2.setId(3L);
        child2.setParentId(1L);
        child2.setName("Child2");
        child2.setPath("/child2");

        List<MenuDTO> flatList = List.of(root, child1, child2);

        List<MenuDTO> tree = menuService.buildMenuTree(flatList);

        assertEquals(1, tree.size());
        assertEquals(2, tree.getFirst().getChildren().size());
    }

    @Test
    void buildMenuTree_BranchCoverage() {
        MenuDTO leafPathNull = new MenuDTO();
        leafPathNull.setId(1L);
        leafPathNull.setPath(null);

        MenuDTO leafPathEmpty = new MenuDTO();
        leafPathEmpty.setId(2L);
        leafPathEmpty.setPath("");

        MenuDTO leafPathValid = new MenuDTO();
        leafPathValid.setId(3L);
        leafPathValid.setPath("/valid");

        MenuDTO parentEmptyPathNull = new MenuDTO();
        parentEmptyPathNull.setId(4L);
        parentEmptyPathNull.setPath(null);
        
        MenuDTO childOf4 = new MenuDTO();
        childOf4.setId(41L);
        childOf4.setParentId(4L);
        childOf4.setPath(null);

        MenuDTO parentEmptyPathValid = new MenuDTO();
        parentEmptyPathValid.setId(5L);
        parentEmptyPathValid.setPath("/parent-valid");

        MenuDTO childOf5 = new MenuDTO();
        childOf5.setId(51L);
        childOf5.setParentId(5L);
        childOf5.setPath(null);

        MenuDTO parentValidChildren = new MenuDTO();
        parentValidChildren.setId(6L);
        parentValidChildren.setPath(null);

        MenuDTO childOf6 = new MenuDTO();
        childOf6.setId(61L);
        childOf6.setParentId(6L);
        childOf6.setPath("/child-valid");

        List<MenuDTO> list = List.of(
            leafPathNull, leafPathEmpty, leafPathValid,
            parentEmptyPathNull, childOf4,
            parentEmptyPathValid, childOf5,
            parentValidChildren, childOf6
        );

        List<MenuDTO> tree = menuService.buildMenuTree(list);

        assertEquals(3, tree.size());
        assertTrue(tree.stream().anyMatch(m -> m.getId().equals(3L)));
        assertTrue(tree.stream().anyMatch(m -> m.getId().equals(5L)));
        assertTrue(tree.stream().anyMatch(m -> m.getId().equals(6L)));
    }

    @Test
    void buildMenuTree_ExplicitEmptyChildren() {
        MenuDTO menu = new MenuDTO();
        menu.setId(1L);
        menu.setPath("/path");
        menu.setChildren(new ArrayList<>());

        List<MenuDTO> list = List.of(menu);

        List<MenuDTO> tree = menuService.buildMenuTree(list);

        assertEquals(1, tree.size());
        assertNotNull(tree.getFirst().getChildren());
        assertTrue(tree.getFirst().getChildren().isEmpty());
    }

    @Test
    void buildMenuTree_ExplicitEmptyChildren_NoPath() {
        MenuDTO menu = new MenuDTO();
        menu.setId(1L);
        menu.setPath(null);
        menu.setChildren(new ArrayList<>());

        List<MenuDTO> list = List.of(menu);

        List<MenuDTO> tree = menuService.buildMenuTree(list);

        assertTrue(tree.isEmpty());
    }

    @Test
    void buildMenuTree_RecursiveFiltering_ParentRemoved() {
        MenuDTO parent = new MenuDTO();
        parent.setId(1L);
        parent.setName("Parent");
        parent.setPath("");

        MenuDTO child = new MenuDTO();
        child.setId(2L);
        child.setParentId(1L);
        child.setName("Child");
        child.setIsHidden(true);

        List<MenuDTO> list = List.of(parent, child);

        List<MenuDTO> tree = menuService.buildMenuTree(list);

        assertTrue(tree.isEmpty());
    }

    @Test
    void buildMenuTree_RecursiveFiltering_ParentKeptWithPath() {
        MenuDTO parent = new MenuDTO();
        parent.setId(1L);
        parent.setName("Parent");
        parent.setPath("/parent");

        MenuDTO child = new MenuDTO();
        child.setId(2L);
        child.setParentId(1L);
        child.setName("Child");
        child.setIsHidden(true);

        List<MenuDTO> list = List.of(parent, child);

        List<MenuDTO> tree = menuService.buildMenuTree(list);

        assertEquals(1, tree.size());
        assertEquals("Parent", tree.getFirst().getName());
        assertTrue(tree.getFirst().getChildren() == null || tree.getFirst().getChildren().isEmpty());
    }

    @Test
    void buildMenuTree_ChildFilteredOut_ParentKept() {
        MenuDTO parent = new MenuDTO();
        parent.setId(1L);
        parent.setName("Parent");
        parent.setPath("/parent");

        MenuDTO child = new MenuDTO();
        child.setId(2L);
        child.setParentId(1L);
        child.setName("Child");
        child.setPath("");

        List<MenuDTO> list = List.of(parent, child);

        List<MenuDTO> tree = menuService.buildMenuTree(list);

        assertEquals(1, tree.size());
        assertEquals("Parent", tree.getFirst().getName());
        assertNotNull(tree.getFirst().getChildren());
        assertTrue(tree.getFirst().getChildren().isEmpty());
    }

    @Test
    void buildMenuAndPermissionTree_SuperTenant() throws Exception {
        Menu menu1 = mock(Menu.class);
        when(menu1.getId()).thenReturn(1L);
        when(menu1.getName()).thenReturn("Menu1");
        when(menu1.getParent()).thenReturn(null);
        when(menu1.getPermissions()).thenReturn(Collections.emptySet());
        when(menu1.getPath()).thenReturn("/menu1");

        when(menuRepository.findAll()).thenReturn(List.of(menu1));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() -> 
            menuService.buildMenuAndPermissionTree()
        );
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getId());
    }

    @Test
    void buildMenuAndPermissionTree_NormalTenant() throws Exception {
        String tenantId = "tenant1";
        Long packageId = 100L;

        com.mok.domain.sys.model.Tenant tenant = mock(com.mok.domain.sys.model.Tenant.class);
        when(tenant.getPackageId()).thenReturn(packageId);
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        when(tenantPackageService.getMenuIdsByPackage(packageId)).thenReturn(Set.of(1L));
        when(tenantPackageService.getPermissionIdsByPackage(packageId)).thenReturn(Collections.emptySet());

        Menu menu1 = mock(Menu.class);
        when(menu1.getId()).thenReturn(1L);
        when(menu1.getName()).thenReturn("Menu1");
        when(menu1.getParent()).thenReturn(null);
        when(menu1.getPath()).thenReturn("/menu1");
        
        Menu menu2 = mock(Menu.class);
        when(menu2.getId()).thenReturn(2L);

        when(menuRepository.findAll()).thenReturn(List.of(menu1, menu2));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, tenantId).call(() -> 
            menuService.buildMenuAndPermissionTree()
        );
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getId());
    }

    @Test
    void buildMenuAndPermissionTree_NormalTenant_WithPermissions() throws Exception {
        String tenantId = "tenant1";
        Long packageId = 100L;

        com.mok.domain.sys.model.Tenant tenant = mock(com.mok.domain.sys.model.Tenant.class);
        when(tenant.getPackageId()).thenReturn(packageId);
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        when(tenantPackageService.getMenuIdsByPackage(packageId)).thenReturn(Set.of(1L));
        when(tenantPackageService.getPermissionIdsByPackage(packageId)).thenReturn(Set.of(10L));

        Menu menu1 = mock(Menu.class);
        when(menu1.getId()).thenReturn(1L);
        when(menu1.getName()).thenReturn("Menu1");
        when(menu1.getParent()).thenReturn(null);
        when(menu1.getPath()).thenReturn("/menu1");

        Permission p1 = mock(Permission.class);
        when(p1.getId()).thenReturn(10L);
        when(p1.getName()).thenReturn("Allowed");

        Permission p2 = mock(Permission.class);
        when(p2.getId()).thenReturn(11L);
        when(p2.getName()).thenReturn("Denied");

        when(menu1.getPermissions()).thenReturn(Set.of(p1, p2));

        when(menuRepository.findAll()).thenReturn(List.of(menu1));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, tenantId).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        MenuOptionDTO dto = result.getFirst();
        assertNotNull(dto.getPermissions());
        assertEquals(1, dto.getPermissions().size());
        assertEquals(10L, dto.getPermissions().getFirst().getId());
    }

    @Test
    void buildMenuAndPermissionTree_NormalTenant_NullPermissions() throws Exception {
        String tenantId = "tenant1";
        Long packageId = 100L;

        com.mok.domain.sys.model.Tenant tenant = mock(com.mok.domain.sys.model.Tenant.class);
        when(tenant.getPackageId()).thenReturn(packageId);
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        when(tenantPackageService.getMenuIdsByPackage(packageId)).thenReturn(Set.of(1L));
        when(tenantPackageService.getPermissionIdsByPackage(packageId)).thenReturn(Collections.emptySet());

        Menu menu1 = mock(Menu.class);
        when(menu1.getId()).thenReturn(1L);
        when(menu1.getName()).thenReturn("Menu1");
        when(menu1.getParent()).thenReturn(null);
        when(menu1.getPath()).thenReturn("/menu1");
        when(menu1.getPermissions()).thenReturn(null);

        when(menuRepository.findAll()).thenReturn(List.of(menu1));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, tenantId).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        assertNull(result.getFirst().getPermissions());
    }

    @Test
    void buildMenuAndPermissionTree_NormalTenant_NoPackage() throws Exception {
        String tenantId = "tenant1";
        com.mok.domain.sys.model.Tenant tenant = mock(com.mok.domain.sys.model.Tenant.class);
        when(tenant.getPackageId()).thenReturn(null);
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        when(menuRepository.findAll()).thenReturn(List.of(mock(Menu.class)));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, tenantId).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void buildMenuAndPermissionTree_WithButtons() throws Exception {
        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(1L);
        when(menu.getName()).thenReturn("Menu");
        when(menu.getPath()).thenReturn("/menu");

        Permission perm = mock(Permission.class);
        when(perm.getId()).thenReturn(10L);
        when(perm.getName()).thenReturn("Btn");

        when(menu.getPermissions()).thenReturn(Set.of(perm));
        when(menuRepository.findAll()).thenReturn(List.of(menu));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        MenuOptionDTO dto = result.getFirst();
        assertNotNull(dto.getChildren());
        assertEquals(1, dto.getChildren().size());
        assertEquals("[按钮] Btn", dto.getChildren().getFirst().getName());
        assertTrue(dto.getChildren().getFirst().getIsPermission());
    }

    @Test
    void buildMenuAndPermissionTree_NullPermissions() throws Exception {
        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(1L);
        when(menu.getName()).thenReturn("Menu");
        when(menu.getPath()).thenReturn("/menu");
        when(menu.getPermissions()).thenReturn(null);

        when(menuRepository.findAll()).thenReturn(List.of(menu));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        assertNull(result.getFirst().getPermissions());
    }

    @Test
    void buildMenuAndPermissionTree_ChildBeforeParent_ParentHasPermissions() throws Exception {
        Menu child = mock(Menu.class);
        when(child.getId()).thenReturn(2L);
        when(child.getName()).thenReturn("Child");
        when(child.getPath()).thenReturn("/child");
        Menu parentRef = mock(Menu.class);
        when(parentRef.getId()).thenReturn(1L);
        when(child.getParent()).thenReturn(parentRef);

        Menu parent = mock(Menu.class);
        when(parent.getId()).thenReturn(1L);
        when(parent.getName()).thenReturn("Parent");
        when(parent.getPath()).thenReturn("/parent");
        when(parent.getParent()).thenReturn(null);

        Permission perm = mock(Permission.class);
        when(perm.getId()).thenReturn(10L);
        when(perm.getName()).thenReturn("Btn");
        when(parent.getPermissions()).thenReturn(Set.of(perm));

        when(menuRepository.findAll()).thenReturn(List.of(child, parent));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        MenuOptionDTO parentDto = result.getFirst();
        assertEquals("Parent", parentDto.getName());
        
        assertNotNull(parentDto.getChildren());
        assertEquals(2, parentDto.getChildren().size());
        
        boolean hasChildMenu = parentDto.getChildren().stream().anyMatch(c -> "Child".equals(c.getName()));
        boolean hasButton = parentDto.getChildren().stream().anyMatch(c -> "[按钮] Btn".equals(c.getName()));
        
        assertTrue(hasChildMenu);
        assertTrue(hasButton);
    }

    @Test
    void buildMenuAndPermissionTree_Orphan() throws Exception {
        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(1L);
        when(menu.getName()).thenReturn("Orphan");
        when(menu.getPath()).thenReturn("/orphan");
        
        Menu parentRef = mock(Menu.class);
        when(parentRef.getId()).thenReturn(999L);
        when(menu.getParent()).thenReturn(parentRef);

        when(menuRepository.findAll()).thenReturn(List.of(menu));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void buildMenuAndPermissionTree_NormalTenant_WithParent() throws Exception {
        String tenantId = "tenant1";
        Long packageId = 100L;

        com.mok.domain.sys.model.Tenant tenant = mock(com.mok.domain.sys.model.Tenant.class);
        when(tenant.getPackageId()).thenReturn(packageId);
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));

        when(tenantPackageService.getMenuIdsByPackage(packageId)).thenReturn(Set.of(1L, 2L));
        when(tenantPackageService.getPermissionIdsByPackage(packageId)).thenReturn(Collections.emptySet());

        Menu parent = mock(Menu.class);
        when(parent.getId()).thenReturn(1L);
        when(parent.getName()).thenReturn("Parent");
        when(parent.getParent()).thenReturn(null);
        when(parent.getPath()).thenReturn("/parent");

        Menu child = mock(Menu.class);
        when(child.getId()).thenReturn(2L);
        when(child.getName()).thenReturn("Child");
        when(child.getParent()).thenReturn(parent);
        when(child.getPath()).thenReturn("/child");

        when(menuRepository.findAll()).thenReturn(List.of(parent, child));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, tenantId).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        assertEquals("Parent", result.getFirst().getName());
        assertEquals(1, result.getFirst().getChildren().size());
        assertEquals("Child", result.getFirst().getChildren().getFirst().getName());
        assertEquals(1L, result.getFirst().getChildren().getFirst().getParentId());
    }

    @Test
    void buildMenuAndPermissionTree_ParentIdZero() throws Exception {
        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(1L);
        when(menu.getName()).thenReturn("Root");
        when(menu.getPath()).thenReturn("/root");
        
        Menu parentZero = mock(Menu.class);
        when(parentZero.getId()).thenReturn(0L);
        when(menu.getParent()).thenReturn(parentZero);

        when(menuRepository.findAll()).thenReturn(List.of(menu));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        assertEquals("Root", result.getFirst().getName());
    }

    @Test
    void buildMenuAndPermissionTree_TwoSubMenus() throws Exception {
        Menu parent = mock(Menu.class);
        when(parent.getId()).thenReturn(1L);
        when(parent.getName()).thenReturn("Parent");
        when(parent.getPath()).thenReturn("/parent");
        when(parent.getParent()).thenReturn(null);

        Menu child1 = mock(Menu.class);
        when(child1.getId()).thenReturn(2L);
        when(child1.getName()).thenReturn("Child1");
        when(child1.getPath()).thenReturn("/child1");
        when(child1.getParent()).thenReturn(parent);

        Menu child2 = mock(Menu.class);
        when(child2.getId()).thenReturn(3L);
        when(child2.getName()).thenReturn("Child2");
        when(child2.getPath()).thenReturn("/child2");
        when(child2.getParent()).thenReturn(parent);

        when(menuRepository.findAll()).thenReturn(List.of(parent, child1, child2));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().getChildren().size());
    }

    @Test
    void buildMenuAndPermissionTree_FilterBranches() throws Exception {
        Menu m1 = mock(Menu.class);
        when(m1.getId()).thenReturn(1L);
        when(m1.getPath()).thenReturn(null);
        when(m1.getParent()).thenReturn(null);
        when(m1.getPermissions()).thenReturn(Collections.emptySet());

        Menu m2 = mock(Menu.class);
        when(m2.getId()).thenReturn(2L);
        when(m2.getPath()).thenReturn("/path");
        when(m2.getParent()).thenReturn(null);
        when(m2.getPermissions()).thenReturn(Collections.emptySet());

        Menu m3 = mock(Menu.class);
        when(m3.getId()).thenReturn(3L);
        when(m3.getPath()).thenReturn(null);
        when(m3.getParent()).thenReturn(null);
        when(m3.getPermissions()).thenReturn(Collections.emptySet());

        Menu m3Child = mock(Menu.class);
        when(m3Child.getId()).thenReturn(31L);
        when(m3Child.getPath()).thenReturn("/child");
        when(m3Child.getParent()).thenReturn(m3);
        when(m3Child.getPermissions()).thenReturn(Collections.emptySet());

        Menu m4 = mock(Menu.class);
        when(m4.getId()).thenReturn(4L);
        when(m4.getPath()).thenReturn(null);
        when(m4.getParent()).thenReturn(null);
        
        Permission p = mock(Permission.class);
        when(p.getId()).thenReturn(41L);
        when(p.getName()).thenReturn("Btn");
        when(m4.getPermissions()).thenReturn(Set.of(p));

        Menu m5 = mock(Menu.class);
        when(m5.getId()).thenReturn(5L);
        when(m5.getPath()).thenReturn("");
        when(m5.getParent()).thenReturn(null);
        when(m5.getPermissions()).thenReturn(Collections.emptySet());

        when(menuRepository.findAll()).thenReturn(List.of(m1, m2, m3, m3Child, m4, m5));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(m -> m.getId().equals(2L)));
        assertTrue(result.stream().anyMatch(m -> m.getId().equals(3L)));
        assertTrue(result.stream().anyMatch(m -> m.getId().equals(4L)));
    }

    @Test
    void buildMenuAndPermissionTree_RecursiveFiltering_ChildRemoved() throws Exception {
        Menu parent = mock(Menu.class);
        when(parent.getId()).thenReturn(1L);
        when(parent.getName()).thenReturn("Parent");
        when(parent.getPath()).thenReturn("/parent");
        when(parent.getParent()).thenReturn(null);
        when(parent.getPermissions()).thenReturn(Collections.emptySet());

        Menu child = mock(Menu.class);
        when(child.getId()).thenReturn(2L);
        when(child.getName()).thenReturn("Child");
        when(child.getPath()).thenReturn(null);
        when(child.getParent()).thenReturn(parent);
        when(child.getPermissions()).thenReturn(Collections.emptySet());

        when(menuRepository.findAll()).thenReturn(List.of(parent, child));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() ->
                menuService.buildMenuAndPermissionTree()
        );

        assertEquals(1, result.size());
        assertEquals("Parent", result.getFirst().getName());
        assertNotNull(result.getFirst().getChildren());
        assertTrue(result.getFirst().getChildren().isEmpty());
    }

    @Test
    void buildMenuAndPermissionTree_ExplicitEmptyChildren() throws Exception {
        Menu menu = mock(Menu.class);
        when(menu.getId()).thenReturn(1L);
        when(menu.getName()).thenReturn("Menu");
        when(menu.getPath()).thenReturn("/menu");
        when(menu.getPermissions()).thenReturn(Collections.emptySet());

        when(menuRepository.findAll()).thenReturn(List.of(menu));

        List<MenuOptionDTO> result = ScopedValue.where(TenantContextHolder.TENANT_ID, Const.SUPER_TENANT_ID).call(() -> {
            List<MenuOptionDTO> flatList = new ArrayList<>();
            MenuOptionDTO dto = new MenuOptionDTO();
            dto.setId(1L);
            dto.setPath("/menu");
            dto.setChildren(new ArrayList<>());
            flatList.add(dto);
            
            try {
                java.lang.reflect.Method method = MenuService.class.getDeclaredMethod("buildTreeFromFlatList", List.class);
                method.setAccessible(true);
                return (List<MenuOptionDTO>) method.invoke(menuService, flatList);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(1, result.size());
        assertNotNull(result.getFirst().getChildren());
        assertTrue(result.getFirst().getChildren().isEmpty());
    }
}
