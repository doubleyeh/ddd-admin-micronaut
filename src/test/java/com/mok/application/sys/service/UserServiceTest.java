package com.mok.application.sys.service;

import com.mok.application.exception.BizException;
import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.auth.AccountInfoDTO;
import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.dto.user.UserDTO;
import com.mok.application.sys.dto.user.UserPasswordDTO;
import com.mok.application.sys.dto.user.UserPostDTO;
import com.mok.application.sys.dto.user.UserPutDTO;
import com.mok.application.sys.dto.user.UserQuery;
import com.mok.application.sys.mapper.MenuMapper;
import com.mok.application.sys.mapper.UserMapper;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.model.Permission;
import com.mok.domain.sys.model.Role;
import com.mok.domain.sys.model.Tenant;
import com.mok.domain.sys.model.TenantPackage;
import com.mok.domain.sys.model.User;
import com.mok.domain.sys.repository.*;
import com.mok.infrastructure.common.Const;
import com.mok.infrastructure.sys.security.PasswordEncoder;
import com.mok.infrastructure.tenant.TenantContextHolder;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;
    private MenuService menuService;
    private MenuMapper menuMapper;
    private MenuRepository menuRepository;
    private PermissionService permissionService;
    private TenantRepository tenantRepository;
    private TenantPackageRepository tenantPackageRepository;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        menuService = mock(MenuService.class);
        menuMapper = mock(MenuMapper.class);
        menuRepository = mock(MenuRepository.class);
        permissionService = mock(PermissionService.class);
        tenantRepository = mock(TenantRepository.class);
        tenantPackageRepository = mock(TenantPackageRepository.class);

        userService = new UserService(userRepository, roleRepository, userMapper, passwordEncoder, menuService, menuMapper, menuRepository, permissionService, tenantRepository, tenantPackageRepository);
        
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    @Test
    void findPage_Success() {
        UserQuery query = new UserQuery();
        Pageable pageable = Pageable.unpaged();
        when(userRepository.findUserPage(any(), eq(pageable))).thenReturn(Page.empty());
        
        userService.findPage(query, pageable);
        
        verify(userRepository).findUserPage(any(), eq(pageable));
    }

    @Test
    void getById_Success() {
        Long id = 1L;
        User user = mock(User.class);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(new UserDTO());
        
        userService.getById(id);
        
        verify(userRepository).findById(id);
    }

    @Test
    void getById_NotFound() {
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> userService.getById(id));
    }

    @Test
    void create_Success() {
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("newUser");
        dto.setPassword("password");
        dto.setNickname("Nick");
        dto.setTenantId("tenant1");
        dto.setRoleIds(List.of(1L));

        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn("tenant1");
        when(userRepository.findByTenantIdAndUsername("tenant1", "newUser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(1L);
        when(roleRepository.findByIdIn(new ArrayList<>(dto.getRoleIds()))).thenReturn(Collections.singletonList(role));

        userService.create(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("newUser", savedUser.getUsername());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals("Nick", savedUser.getNickname());
        assertFalse(savedUser.getIsTenantAdmin());
        assertEquals(Const.UserState.NORMAL, savedUser.getState());
        assertEquals(1, savedUser.getRoles().size());
        assertTrue(savedUser.getRoles().stream().anyMatch(r -> r.getId().equals(1L)));
    }

    @Test
    void create_NoPermission_ShouldThrowBizException() {
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("newUser");
        dto.setTenantId("tenant2");

        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn("tenant1");

        Exception exception = assertThrows(BizException.class, () -> userService.create(dto));
        assertEquals("无权限管理其他用户", exception.getMessage());
    }
    
    @Test
    void create_UsernameExists_ShouldThrowBizException() {
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("existingUser");
        dto.setTenantId("tenant1");

        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn("tenant1");
        when(userRepository.findByTenantIdAndUsername("tenant1", "existingUser")).thenReturn(Optional.of(mock(User.class)));

        Exception exception = assertThrows(BizException.class, () -> userService.create(dto));
        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void createForTenant_Success() {
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("tenantAdmin");
        dto.setPassword("password");
        dto.setNickname("Admin");
        dto.setTenantId("newTenant");
        dto.setRoleIds(List.of(1L));

        when(userRepository.findByTenantIdAndUsername("newTenant", "tenantAdmin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(1L);
        when(roleRepository.findByIdIn(new ArrayList<>(dto.getRoleIds()))).thenReturn(Collections.singletonList(role));

        userService.createForTenant(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("tenantAdmin", savedUser.getUsername());
        assertTrue(savedUser.getIsTenantAdmin());
        assertEquals("newTenant", savedUser.getTenantId());
    }

    @Test
    void createForTenant_NoTenantId_ShouldThrowBizException() {
        UserPostDTO dto = new UserPostDTO();
        dto.setTenantId(null);

        Exception exception = assertThrows(BizException.class, () -> userService.createForTenant(dto));
        assertEquals("创建租户用户时，租户ID不能为空", exception.getMessage());
    }

    @Test
    void updateUser_Success() {
        Long userId = 1L;
        UserPutDTO dto = new UserPutDTO();
        dto.setId(userId);
        dto.setNickname("new nickname");
        dto.setRoleIds(Collections.singletonList(2L));

        User existingUser = User.create("oldUser", "pass", "old nickname", false);

        Role newRole = mock(Role.class);
        when(newRole.getId()).thenReturn(2L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByIdIn(new ArrayList<>(dto.getRoleIds()))).thenReturn(Collections.singletonList(newRole));

        userService.updateUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertSame(existingUser, savedUser);
        assertEquals("new nickname", savedUser.getNickname());
        assertEquals(1, savedUser.getRoles().size());
        assertTrue(savedUser.getRoles().stream().anyMatch(r -> r.getId().equals(2L)));
    }

    @Test
    void updateUser_UserNotFound_ShouldThrowNotFoundException() {
        UserPutDTO dto = new UserPutDTO();
        dto.setId(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateUser(dto));
    }

    @Test
    void updateNickname_Success() {
        Long userId = 1L;
        String newNickname = "New Nick";
        User user = User.create("user", "pass", "old", false);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        userService.updateNickname(userId, newNickname);
        
        verify(userRepository).save(user);
        assertEquals(newNickname, user.getNickname());
    }

    @Test
    void updateUserState_Success() {
        Long userId = 1L;
        User user = User.create("user", "pass", "nick", false);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        userService.updateUserState(userId, Const.UserState.DISABLED);
        assertEquals(Const.UserState.DISABLED, user.getState());
        
        userService.updateUserState(userId, Const.UserState.NORMAL);
        assertEquals(Const.UserState.NORMAL, user.getState());
        
        verify(userRepository, times(2)).save(user);
    }

    @Test
    void updatePassword_Success() {
        Long userId = 1L;
        UserPasswordDTO dto = new UserPasswordDTO();
        dto.setId(userId);
        dto.setPassword("newPassword123");

        User existingUser = User.create("user", "oldEncodedPassword", "nick", false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");

        userService.updatePassword(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertSame(existingUser, savedUser);
        assertEquals("newEncodedPassword", savedUser.getPassword());
    }

    @Test
    void deleteById_Success() {
        Long userId = 2L;
        User user = User.create("user", "pass", "nick", false);
        user.setTenantId("tenant1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).deleteById(userId);

        userService.deleteById(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteById_SuperAdmin_ShouldThrowBizException() {
        Long userId = 1L;
        User user = User.create(Const.SUPER_ADMIN_USERNAME, "pass", "super", false);
        user.setTenantId(Const.SUPER_TENANT_ID);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Exception exception = assertThrows(BizException.class, () -> userService.deleteById(userId));
        assertEquals("用户不允许删除", exception.getMessage());
        verify(userRepository, never()).deleteById(any());
    }
    
    @Test
    void deleteById_TenantAdmin_ShouldThrowBizException() {
        Long userId = 1L;
        User user = User.create("admin", "pass", "admin", true);
        user.setTenantId("tenant1");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Exception exception = assertThrows(BizException.class, () -> userService.deleteById(userId));
        assertEquals("租户管理员不允许删除", exception.getMessage());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_NotFound() {
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.deleteById(userId));
    }

    @Test
    void findByUsername_Success() {
        String username = "user";
        User user = mock(User.class);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        
        userService.findByUsername(username);
        
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findAccountInfoByUsername_SuperAdmin() {
        String username = Const.SUPER_ADMIN_USERNAME;
        User user = mock(User.class);
        when(user.getTenantId()).thenReturn(Const.SUPER_TENANT_ID);
        when(user.getUsername()).thenReturn(username);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        
        Menu menu = mock(Menu.class);
        when(menuRepository.findAll()).thenReturn(List.of(menu));
        when(menuMapper.toDto(menu)).thenReturn(new MenuDTO());
        
        when(permissionService.getAllPermissionCodes()).thenReturn(Set.of("perm1"));
        
        AccountInfoDTO result = userService.findAccountInfoByUsername(username);
        
        assertNotNull(result);
        assertTrue(result.getPermissions().contains(Const.SUPER_ADMIN_ROLE_CODE));
        assertTrue(result.getPermissions().contains("perm1"));
    }

    @Test
    void findAccountInfoByUsername_TenantAdmin_WithPackage() {
        String username = "admin";
        String tenantId = "tenant1";
        User user = mock(User.class);
        when(user.getTenantId()).thenReturn(tenantId);
        when(user.getUsername()).thenReturn(username);
        when(user.getIsTenantAdmin()).thenReturn(true);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        
        Tenant tenant = mock(Tenant.class);
        when(tenant.getPackageId()).thenReturn(100L);
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));
        
        TenantPackage pkg = mock(TenantPackage.class);
        Menu menu = mock(Menu.class);
        Permission perm = mock(Permission.class);
        when(perm.getCode()).thenReturn("pkg:perm");
        
        when(pkg.getMenus()).thenReturn(Set.of(menu));
        when(pkg.getPermissions()).thenReturn(Set.of(perm));
        when(tenantPackageRepository.findById(100L)).thenReturn(Optional.of(pkg));
        
        when(menuMapper.toDtoList(any())).thenReturn(List.of(new MenuDTO()));
        
        AccountInfoDTO result = userService.findAccountInfoByUsername(username);
        
        assertNotNull(result);
        assertTrue(result.getPermissions().contains("pkg:perm"));
    }

    @Test
    void findAccountInfoByUsername_TenantAdmin_NoPackage() {
        String username = "admin";
        String tenantId = "tenant1";
        User user = mock(User.class);
        when(user.getTenantId()).thenReturn(tenantId);
        when(user.getUsername()).thenReturn(username);
        when(user.getIsTenantAdmin()).thenReturn(true);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        
        Tenant tenant = mock(Tenant.class);
        when(tenant.getPackageId()).thenReturn(null);
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));
        
        AccountInfoDTO result = userService.findAccountInfoByUsername(username);
        
        assertNotNull(result);
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getMenus().isEmpty());
    }

    @Test
    void findAccountInfoByUsername_TenantAdmin_PackageNotFound() {
        String username = "admin";
        String tenantId = "tenant1";
        User user = mock(User.class);
        when(user.getTenantId()).thenReturn(tenantId);
        when(user.getUsername()).thenReturn(username);
        when(user.getIsTenantAdmin()).thenReturn(true);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        
        Tenant tenant = mock(Tenant.class);
        when(tenant.getPackageId()).thenReturn(100L);
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));
        
        when(tenantPackageRepository.findById(100L)).thenReturn(Optional.empty());
        
        AccountInfoDTO result = userService.findAccountInfoByUsername(username);
        
        assertNotNull(result);
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getMenus().isEmpty());
    }

    @Test
    void findAccountInfoByUsername_NormalUser() {
        String username = "user";
        String tenantId = "tenant1";
        User user = mock(User.class);
        when(user.getTenantId()).thenReturn(tenantId);
        when(user.getUsername()).thenReturn(username);
        when(user.getIsTenantAdmin()).thenReturn(false);
        
        Role role = mock(Role.class);
        Menu menu = mock(Menu.class);
        Permission perm = mock(Permission.class);
        when(perm.getCode()).thenReturn("user:perm");
        
        when(role.getMenus()).thenReturn(Set.of(menu));
        when(role.getPermissions()).thenReturn(Set.of(perm));
        when(user.getRoles()).thenReturn(Set.of(role));
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(menuMapper.toDtoList(any())).thenReturn(List.of(new MenuDTO()));
        
        AccountInfoDTO result = userService.findAccountInfoByUsername(username);
        
        assertNotNull(result);
        assertTrue(result.getPermissions().contains("user:perm"));
    }

    @Test
    void create_WithNullRoles() {
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("newUser");
        dto.setPassword("password");
        dto.setNickname("Nick");
        dto.setTenantId("tenant1");
        dto.setRoleIds(null);

        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn("tenant1");
        when(userRepository.findByTenantIdAndUsername("tenant1", "newUser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        userService.create(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertNull(savedUser.getRoles());
    }

    @Test
    void createForTenant_WithNullRoles() {
        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("tenantAdmin");
        dto.setPassword("password");
        dto.setNickname("Admin");
        dto.setTenantId("newTenant");
        dto.setRoleIds(null);

        when(userRepository.findByTenantIdAndUsername("newTenant", "tenantAdmin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        userService.createForTenant(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertNull(savedUser.getRoles());
    }

    @Test
    void updateUser_WithNullRoles() {
        Long userId = 1L;
        UserPutDTO dto = new UserPutDTO();
        dto.setId(userId);
        dto.setNickname("new nickname");
        dto.setRoleIds(null);

        User existingUser = User.create("oldUser", "pass", "old nickname", false);
        existingUser.setRoles(new HashSet<>());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        userService.updateUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertNull(savedUser.getRoles());
    }

    @Test
    void findAccountInfoByUsername_TenantAdmin_TenantNotFound() {
        String username = "admin";
        String tenantId = "tenant1";
        User user = mock(User.class);
        when(user.getTenantId()).thenReturn(tenantId);
        when(user.getUsername()).thenReturn(username);
        when(user.getIsTenantAdmin()).thenReturn(true);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        AccountInfoDTO result = userService.findAccountInfoByUsername(username);

        assertNotNull(result);
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getMenus().isEmpty());
    }
}
