package com.mok.application.sys.service;

import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.permission.PermissionDTO;
import com.mok.application.sys.dto.permission.PermissionQuery;
import com.mok.application.sys.mapper.PermissionMapper;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.model.Permission;
import com.mok.domain.sys.repository.MenuRepository;
import com.mok.domain.sys.repository.PermissionRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final PermissionMapper permissionMapper;
    private final RedisCommands<String, String> redisCommands;

    @Transactional(readOnly = true)
    public Page<PermissionDTO> findPage(PermissionQuery query, Pageable pageable) {
        Page<Permission> entityPage = permissionRepository.findAll(query.toPredicate(), pageable);
        return entityPage.map(permissionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<PermissionDTO> findAll(PermissionQuery query) {
        List<Permission> list = permissionRepository.findAll(query.toPredicate());

        return list.stream().map(permissionMapper::toDto).toList();
    }

    @Transactional
    public PermissionDTO createPermission(@NonNull PermissionDTO dto) {
        Menu menu = null;
        if (dto.getMenuId() != null) {
            menu = menuRepository.findById(dto.getMenuId()).orElse(null);
        }
        Permission permission = Permission.create(dto.getName(), dto.getCode(), dto.getUrl(), dto.getMethod(), dto.getDescription(), menu);
        return permissionMapper.toDto(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionDTO updatePermission(@NonNull PermissionDTO dto) {
        Permission permission = permissionRepository.findById(dto.getId()).orElseThrow(NotFoundException::new);
        Menu menu = null;
        if (dto.getMenuId() != null) {
            menu = menuRepository.findById(dto.getMenuId()).orElse(null);
        }
        permission.updateInfo(dto.getName(), dto.getCode(), dto.getUrl(), dto.getMethod(), dto.getDescription(), menu);
        return permissionMapper.toDto(permissionRepository.save(permission));
    }

    @Transactional(readOnly = true)
    public Set<String> getAllPermissionCodes() {
        return permissionRepository.findAll().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    public Set<String> getPermissionsByRoleIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Set.of();
        }

        return roleIds.stream().flatMap(roleId -> {
            String cacheKey = Const.CacheKey.ROLE_PERMS + ":" + roleId;
            List<String> cachedPerms = redisCommands.lrange(cacheKey, 0, -1);

            if (cachedPerms == null || cachedPerms.isEmpty()) {
                List<String> dbPerms = permissionRepository.findCodesByRoleId(roleId);
                if (!dbPerms.isEmpty()) {
                    redisCommands.rpush(cacheKey, dbPerms.toArray(new String[0]));
                    return dbPerms.stream();
                }
                return Stream.empty();
            }
            return cachedPerms.stream();
        }).collect(Collectors.toSet());
    }

    @Transactional
    public void deleteById(Long id) {
        List<Long> roleIds = permissionRepository.findRoleIdsByPermissionId(id);

        permissionRepository.deleteRolePermissionsByPermissionId(id);
        permissionRepository.deleteById(id);

        if (CollectionUtils.isNotEmpty(roleIds)) {
            List<String> keys = roleIds.stream()
                    .map(roleId -> Const.CacheKey.ROLE_PERMS + ":" + roleId)
                    .toList();
            redisCommands.del(keys.toArray(new String[0]));
        }
    }
}
