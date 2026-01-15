package com.mok.application.sys.service;

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
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final MenuMapper menuMapper;
    private final RedisCommands<String, String> redisCommands;

    @Transactional(readOnly = true)
    public Page<RoleDTO> findPage(RoleQuery query, Pageable pageable) {
        return roleRepository.findRolePage(query.toPredicate(), pageable);
    }

    @Transactional(readOnly = true)
    public RoleDTO getById(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        RoleDTO dto = roleMapper.toDto(role);

        dto.setMenus(role.getMenus().stream()
                .map(menuMapper::toDto)
                .collect(Collectors.toSet()));

        dto.setPermissions(role.getPermissions().stream()
                .map(permissionMapper::toDto)
                .collect(Collectors.toSet()));
        return dto;
    }

    @Transactional
    public RoleDTO createRole(@NonNull RoleSaveDTO dto) {
        Role entity = Role.create(dto.getName(), dto.getCode(), dto.getDescription(), dto.getSort());
        return roleMapper.toDto(roleRepository.save(entity));
    }

    @Transactional
    public RoleDTO updateRole(@NonNull RoleSaveDTO dto) {
        Role existingRole = roleRepository.findById(dto.getId()).orElseThrow(NotFoundException::new);
        existingRole.updateInfo(dto.getName(), dto.getCode(), dto.getDescription(), dto.getSort());
        return roleMapper.toDto(roleRepository.save(existingRole));
    }

    @Transactional
    public RoleDTO updateState(Long id, Integer state) {
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        if (Objects.equals(state, Const.RoleState.NORMAL)) {
            role.enable();
        } else if (Objects.equals(state, Const.RoleState.DISABLED)) {
            role.disable();
        }
        return roleMapper.toDto(roleRepository.save(role));
    }

    @Transactional
    public void deleteRoleBeforeValidation(Long id) {
        if (roleRepository.existsByRolesId(id)) {
            throw new BizException("该角色下存在用户，请先删除用户关联该角色");
        }
        roleRepository.deleteById(id);
    }

    /**
     * 授权
     */
    @Transactional
    public void grant(Long roleId, RoleGrantDTO dto) {
        Role role = roleRepository.findById(roleId).orElseThrow(NotFoundException::new);

        if (dto.getMenuIds() != null) {
            Set<Menu> menus = new HashSet<>(menuRepository.findByIdIn(dto.getMenuIds()));
            role.changeMenus(menus);
        }

        if (dto.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds()));
            role.changePermissions(permissions);
        }

        roleRepository.save(role);

        String cacheKey = Const.CacheKey.ROLE_PERMS + ":" + roleId;
        redisCommands.del(cacheKey);
    }

    @Transactional(readOnly = true)
    public List<RoleOptionDTO> getRoleOptions(RoleQuery query) {
        return roleRepository.findAll(query.toPredicate()).stream()
                .filter(role -> Objects.equals(role.getState(), Const.RoleState.NORMAL))
                .map(roleMapper::toOptionsDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<MenuDTO> getMenusByRole(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        return role.getMenus().stream()
                .map(menuMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<PermissionDTO> getPermissionsByRole(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(NotFoundException::new);
        return role.getPermissions().stream()
                .map(permissionMapper::toDto)
                .collect(Collectors.toSet());
    }
}
