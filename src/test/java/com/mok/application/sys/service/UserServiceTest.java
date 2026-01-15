package com.mok.application.sys.service;

import com.mok.application.exception.BizException;
import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.user.UserPasswordDTO;
import com.mok.application.sys.dto.user.UserPostDTO;
import com.mok.application.sys.dto.user.UserPutDTO;
import com.mok.application.sys.mapper.MenuMapper;
import com.mok.application.sys.mapper.UserMapper;
import com.mok.domain.sys.model.Role;
import com.mok.domain.sys.model.User;
import com.mok.domain.sys.repository.*;
import com.mok.infrastructure.common.Const;
import com.mok.infrastructure.sys.security.PasswordEncoder;
import com.mok.infrastructure.tenant.TenantContextHolder;
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

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        UserMapper userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        MenuService menuService = mock(MenuService.class);
        MenuMapper menuMapper = mock(MenuMapper.class);
        MenuRepository menuRepository = mock(MenuRepository.class);
        PermissionService permissionService = mock(PermissionService.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        TenantPackageRepository tenantPackageRepository = mock(TenantPackageRepository.class);

        userService = new UserService(userRepository, roleRepository, userMapper, passwordEncoder, menuService, menuMapper, menuRepository, permissionService, tenantRepository, tenantPackageRepository);
        
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
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
}
