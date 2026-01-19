package com.mok.sys.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mok.common.application.exception.BizException;
import com.mok.sys.application.dto.tenantPackage.*;
import com.mok.sys.application.mapper.MenuMapper;
import com.mok.sys.application.mapper.PermissionMapper;
import com.mok.sys.application.mapper.TenantPackageMapper;
import com.mok.sys.domain.model.Menu;
import com.mok.sys.domain.model.Permission;
import com.mok.sys.domain.model.TenantPackage;
import com.mok.sys.domain.repository.MenuRepository;
import com.mok.sys.domain.repository.PermissionRepository;
import com.mok.sys.domain.repository.TenantPackageRepository;
import com.mok.sys.domain.repository.TenantRepository;
import com.mok.common.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class TenantPackageService {

    private final TenantPackageRepository packageRepository;
    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;
    private final TenantPackageMapper packageMapper;
    private final TenantRepository tenantRepository;
    private final RedisCommands<String, String> redisCommands;
    private final MenuMapper menuMapper;
    private final PermissionMapper permissionMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<TenantPackageDTO> findPage(TenantPackageQuery query, Pageable pageable) {
        Page<TenantPackage> entityPage = packageRepository.findAll(query.toPredicate(), pageable);
        return entityPage.map(packageMapper::toDto);
    }

    @Transactional
    public void createPackage(TenantPackageSaveDTO dto) {
        TenantPackage entity = TenantPackage.create(dto.getName(), dto.getDescription());
        packageRepository.save(entity);
    }

    @Transactional
    public void updatePackage(Long id, TenantPackageSaveDTO dto) {
        TenantPackage entity = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("套餐不存在"));
        entity.updateInfo(dto.getName(), dto.getDescription());
        packageRepository.save(entity);
    }

    @Transactional
    public void grant(Long id, TenantPackageGrantDTO dto) {
        TenantPackage entity = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("套餐不存在"));

        if (dto.getMenuIds() != null) {
            Set<Menu> menus = new HashSet<>(menuRepository.findByIdIn(dto.getMenuIds()));
            entity.changeMenus(menus);
        }
        if (dto.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds()));
            entity.changePermissions(permissions);
        }
        packageRepository.save(entity);

        // 清理缓存
        String[] keys = {
                Const.CacheKey.TENANT_PACKAGE_PERMS + ":menus:" + id,
                Const.CacheKey.TENANT_PACKAGE_PERMS + ":permissions:" + id
        };
        redisCommands.del(keys);
    }

    @Transactional
    public TenantPackageDTO updateTenantState(@NonNull Long id, @NonNull Integer state) {
        TenantPackage existingTenant = packageRepository.findById(id).orElseThrow(() -> new BizException("套餐不存在"));
        if (Objects.equals(state, Const.TenantPackageState.NORMAL)) {
            existingTenant.enable();
        } else {
            existingTenant.disable();
        }
        return packageMapper.toDto(packageRepository.save(existingTenant));
    }

    @Transactional
    public void deleteByVerify(@NonNull Long id) {
        packageRepository.findById(id).orElseThrow(() -> new BizException("套餐不存在"));
        long useCount = tenantRepository.countByPackageId(id);
        if (useCount > 0) {
            throw new BizException("套餐正在使用中，不允许删除");
        }
        packageRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TenantPackageOptionDTO> findOptions(String name) {
        List<TenantPackage> packages;
        if (StringUtils.isNotEmpty(name)) {
            packages = packageRepository.findByNameContainsIgnoreCaseAndState(name, Const.TenantPackageState.NORMAL);
        } else {
            packages = packageRepository.findByState(Const.TenantPackageState.NORMAL);
        }
        return packages.stream()
                .map(packageMapper::toDto)
                .map(dto -> {
                    TenantPackageOptionDTO option = new TenantPackageOptionDTO();
                    option.setId(dto.getId());
                    option.setName(dto.getName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<Long> getMenuIdsByPackage(Long id) {
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":menus:" + id;
        String cached = redisCommands.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<Set<Long>>() {
                });
            } catch (IOException e) {
                // Log error
            }
        }

        TenantPackage tenantPackage = packageRepository.findById(id).orElse(null);
        if (tenantPackage == null || tenantPackage.getMenus() == null) {
            return Collections.emptySet();
        }
        Set<Long> menuIds = tenantPackage.getMenus().stream().map(Menu::getId).collect(Collectors.toSet());
        try {
            redisCommands.set(cacheKey, objectMapper.writeValueAsString(menuIds));
        } catch (IOException e) {
            // Log error
        }
        return menuIds;
    }

    @Transactional(readOnly = true)
    public Set<Long> getPermissionIdsByPackage(Long id) {
        String cacheKey = Const.CacheKey.TENANT_PACKAGE_PERMS + ":permissions:" + id;
        String cached = redisCommands.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<Set<Long>>() {
                });
            } catch (IOException e) {
                // Log error
            }
        }

        TenantPackage tenantPackage = packageRepository.findById(id).orElse(null);
        if (tenantPackage == null || tenantPackage.getPermissions() == null) {
            return Collections.emptySet();
        }
        Set<Long> permissionIds = tenantPackage.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
        try {
            redisCommands.set(cacheKey, objectMapper.writeValueAsString(permissionIds));
        } catch (IOException e) {
            // Log error
        }
        return permissionIds;
    }

    @Transactional(readOnly = true)
    public TenantPackageDTO getById(Long id) {
        TenantPackage entity = packageRepository.findById(id)
                .orElseThrow(() -> new BizException("套餐不存在"));
        TenantPackageDTO dto = packageMapper.toDto(entity);

        if (entity.getMenus() != null) {
            dto.setMenus(entity.getMenus().stream()
                    .map(menuMapper::toDto)
                    .collect(Collectors.toSet()));
        } else {
            dto.setMenus(Collections.emptySet());
        }

        if (entity.getPermissions() != null) {
            dto.setPermissions(entity.getPermissions().stream()
                    .map(permissionMapper::toDto)
                    .collect(Collectors.toSet()));
        } else {
            dto.setPermissions(Collections.emptySet());
        }
        return dto;
    }
}
