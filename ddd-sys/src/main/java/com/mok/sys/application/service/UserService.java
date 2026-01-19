package com.mok.sys.application.service;

import com.mok.common.application.exception.BizException;
import com.mok.common.application.exception.NotFoundException;
import com.mok.sys.application.dto.auth.AccountInfoDTO;
import com.mok.sys.application.dto.menu.MenuDTO;
import com.mok.sys.application.dto.user.UserDTO;
import com.mok.sys.application.dto.user.UserPostDTO;
import com.mok.sys.application.dto.user.UserPutDTO;
import com.mok.sys.application.dto.user.UserQuery;
import com.mok.sys.application.dto.user.UserPasswordDTO;
import com.mok.sys.application.mapper.MenuMapper;
import com.mok.sys.application.mapper.UserMapper;
import com.mok.sys.domain.model.*;
import com.mok.sys.domain.repository.*;
import com.mok.common.infrastructure.common.Const;
import com.mok.sys.infrastructure.sys.security.PasswordEncoder;
import com.mok.common.infrastructure.tenant.TenantContextHolder;
import com.mok.common.infrastructure.util.SysUtil;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MenuService menuService;
    private final MenuMapper menuMapper;
    private final MenuRepository menuRepository;
    private final PermissionService permissionService;
    private final TenantRepository tenantRepository;
    private final TenantPackageRepository tenantPackageRepository;

    @Transactional(readOnly = true)
    public Page<UserDTO> findPage(UserQuery query, Pageable pageable) {
        return userRepository.findUserPage(query.toPredicate(), pageable);
    }

    @Transactional(readOnly = true)
    public UserDTO getById(Long id) {
        return userMapper.toDto(userRepository.findById(id).orElseThrow(() -> new NotFoundException(Const.NOT_FOUND_MESSAGE)));
    }

    @Transactional
    public UserDTO create(@NonNull UserPostDTO dto) {
        if (!SysUtil.isSuperTenant(TenantContextHolder.getTenantId()) && !Objects.equals(dto.getTenantId(), TenantContextHolder.getTenantId())) {
            throw new BizException("无权限管理其他用户");
        }
        if (userRepository.findByTenantIdAndUsername(dto.getTenantId(), dto.getUsername()).isPresent()) {
            throw new BizException("用户名已存在");
        }
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        Objects.requireNonNull(encodedPassword, "密码加密失败");
        User entity = User.create(dto.getUsername(), encodedPassword, dto.getNickname(), false);

        if (dto.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findByIdIn(dto.getRoleIds()));
            entity.changeRoles(roles);
        }

        return userMapper.toDto(userRepository.save(entity));
    }

    @Transactional
    public UserDTO createForTenant(@NonNull UserPostDTO dto) {
        String tenantId = dto.getTenantId();
        if (tenantId == null) {
            throw new BizException("创建租户用户时，租户ID不能为空");
        }
        if (userRepository.findByTenantIdAndUsername(tenantId, dto.getUsername()).isPresent()) {
            throw new BizException("用户名已存在");
        }
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        Objects.requireNonNull(encodedPassword, "密码加密失败");
        User entity = User.create(dto.getUsername(), encodedPassword, dto.getNickname(), true);
        entity.assignTenant(tenantId);

        if (dto.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findByIdIn(dto.getRoleIds()));
            entity.changeRoles(roles);
        }

        return userMapper.toDto(userRepository.save(entity));
    }

    @Transactional
    public UserDTO updateUser(@NonNull UserPutDTO dto) {
        User entity = userRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException(Const.NOT_FOUND_MESSAGE));

        Set<Role> roles = null;
        if (dto.getRoleIds() != null) {
            roles = new HashSet<>(roleRepository.findByIdIn(dto.getRoleIds()));
        }
        entity.updateInfo(dto.getNickname(), roles);
        return userMapper.toDto(userRepository.save(entity));
    }

    @Transactional
    public void updateNickname(@NonNull Long id, String nickname) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(Const.NOT_FOUND_MESSAGE));
        user.updateInfo(nickname, user.getRoles());
        userRepository.save(user);
    }

    @Transactional
    public UserDTO updateUserState(@NonNull Long id, @NonNull Integer state) {
        User entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException(Const.NOT_FOUND_MESSAGE));
        if (Objects.equals(state, Const.UserState.NORMAL)) {
            entity.enable();
        } else {
            entity.disable();
        }
        return userMapper.toDto(userRepository.save(entity));
    }

    @Transactional
    public void updatePassword(@NonNull UserPasswordDTO dto) {
        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException(Const.NOT_FOUND_MESSAGE));
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        Objects.requireNonNull(encodedPassword, "密码加密失败");
        user.changePassword(encodedPassword);
        userRepository.save(user);
    }

    @Transactional
    public void deleteById(@NonNull Long id) {
        Optional<User> userToDelete = userRepository.findById(id);

        if (userToDelete.isEmpty()) {
            throw new NotFoundException();
        }
        if (SysUtil.isSuperAdmin(userToDelete.get().getTenantId(), userToDelete.get().getUsername())) {
            throw new BizException("用户不允许删除");
        }
        if (Boolean.TRUE.equals(userToDelete.get().getIsTenantAdmin())) {
            throw new BizException("租户管理员不允许删除");
        }

        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public UserDTO findByUsername(String username) {
        return userMapper.toDto(userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(Const.NOT_FOUND_MESSAGE)));
    }

    @Transactional(readOnly = true)
    public AccountInfoDTO findAccountInfoByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(Const.NOT_FOUND_MESSAGE));

        List<MenuDTO> flatMenus;
        Set<String> distinctPermissions;

        if (SysUtil.isSuperAdmin(user.getTenantId(), username)) {
            // 超级管理员拥有所有权限
            flatMenus = menuRepository.findAll().stream().map(menuMapper::toDto).collect(Collectors.toList());
            distinctPermissions = new HashSet<>(permissionService.getAllPermissionCodes());
            distinctPermissions.add(Const.SUPER_ADMIN_ROLE_CODE);
        } else if (Boolean.TRUE.equals(user.getIsTenantAdmin())) {
            // 租户管理员，拥有该租户套餐下的所有权限
            Long packageId = tenantRepository.findByTenantId(user.getTenantId())
                    .map(Tenant::getPackageId)
                    .orElse(null);

            if (packageId != null) {
                TenantPackage tenantPackage = tenantPackageRepository.findById(packageId).orElse(null);
                if (tenantPackage != null) {
                    flatMenus = new ArrayList<>(menuMapper.toDtoList(tenantPackage.getMenus()));
                    distinctPermissions = tenantPackage.getPermissions().stream()
                            .map(Permission::getCode)
                            .collect(Collectors.toSet());
                } else {
                    flatMenus = new ArrayList<>();
                    distinctPermissions = new HashSet<>();
                }
            } else {
                flatMenus = new ArrayList<>();
                distinctPermissions = new HashSet<>();
            }
        } else {
            // 普通用户，根据角色获取权限
            Set<Menu> distinctMenuEntities = user.getRoles().stream()
                    .flatMap(role -> role.getMenus().stream())
                    .collect(Collectors.toSet());

            flatMenus = new ArrayList<>(menuMapper.toDtoList(distinctMenuEntities));

            distinctPermissions = user.getRoles().stream()
                    .flatMap(r -> r.getPermissions().stream())
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());
        }

        flatMenus.sort(Comparator.comparing(MenuDTO::getSort, Comparator.nullsLast(Comparator.naturalOrder())));
        List<MenuDTO> menuTree = menuService.buildMenuTree(flatMenus);

        return AccountInfoDTO.builder()
                .user(userMapper.toDto(user))
                .menus(menuTree)
                .permissions(distinctPermissions).build();
    }
}
