package com.mok.sys.application.service;

import com.mok.common.application.exception.NotFoundException;
import com.mok.sys.application.dto.menu.MenuDTO;
import com.mok.sys.application.dto.menu.MenuOptionDTO;
import com.mok.sys.application.dto.permission.PermissionOptionDTO;
import com.mok.sys.application.mapper.MenuMapper;
import com.mok.sys.domain.model.Menu;
import com.mok.sys.domain.model.Tenant;
import com.mok.sys.domain.model.Permission;
import com.mok.sys.domain.repository.MenuRepository;
import com.mok.sys.domain.repository.PermissionRepository;
import com.mok.sys.domain.repository.TenantRepository;
import com.mok.common.infrastructure.common.Const;
import com.mok.common.infrastructure.tenant.TenantContextHolder;
import com.mok.common.infrastructure.util.SysUtil;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;
    private final RedisCommands<String, String> redisCommands;
    private final MenuMapper menuMapper;
    private final TenantRepository tenantRepository;
    private final TenantPackageService tenantPackageService;

    @Transactional(readOnly = true)
    public List<MenuDTO> findAll() {
        return menuMapper.toDtoList(menuRepository.findAll());
    }

    @Transactional
    public MenuDTO createMenu(@NonNull MenuDTO dto) {
        Menu parent = null;
        if (dto.getParentId() != null) {
            parent = menuRepository.findById(dto.getParentId()).orElse(null);
        }
        Menu menu = Menu.create(parent, dto.getName(), dto.getPath(), dto.getComponent(), dto.getIcon(), dto.getSort(), dto.getIsHidden());
        return menuMapper.toDto(menuRepository.save(menu));
    }

    @Transactional
    public MenuDTO updateMenu(@NonNull MenuDTO dto) {
        Menu menu = menuRepository.findById(dto.getId()).orElseThrow(NotFoundException::new);
        Menu parent = null;
        if (dto.getParentId() != null) {
            parent = menuRepository.findById(dto.getParentId()).orElse(null);
        }
        menu.updateInfo(parent, dto.getName(), dto.getPath(), dto.getComponent(), dto.getIcon(), dto.getSort(), dto.getIsHidden());
        return menuMapper.toDto(menuRepository.save(menu));
    }

    @Transactional
    public void changePermissions(Long menuId, Set<Long> permissionIds) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(NotFoundException::new);

        Set<Permission> newPermissions = new HashSet<>();
        if (CollectionUtils.isNotEmpty(permissionIds)) {
            newPermissions.addAll(permissionRepository.findAllById(permissionIds));
        }

        menu.changePermissions(newPermissions);

        menuRepository.save(menu);
    }

    public List<MenuDTO> buildMenuTree(@NonNull List<MenuDTO> flatList) {
        Map<Long, MenuDTO> dtoMap = flatList.stream()
                .collect(Collectors.toMap(MenuDTO::getId, dto -> dto));

        List<MenuDTO> rootMenus = new ArrayList<>();

        for (MenuDTO dto : flatList) {
            if (Boolean.TRUE.equals(dto.getIsHidden())) {
                continue;
            }

            if (dto.getParentId() == null || dto.getParentId() == 0) {
                rootMenus.add(dto);
            } else {
                MenuDTO parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                }
            }
        }

        return filterEmptyParentMenus(rootMenus);
    }

    @Transactional
    public void deleteById(Long id) {
        List<Long> allIds = getAllMenuIds(id);

        List<Long> roleIds = menuRepository.findRoleIdsByMenuIds(allIds);

        permissionRepository.deleteRolePermissionsByMenuIds(allIds);
        permissionRepository.deleteByMenuIds(allIds);

        menuRepository.deleteRoleMenuByMenuIds(allIds);
        menuRepository.deleteAllById(allIds);

        redisCommands.del(Const.CacheKey.MENU_TREE);
        if (CollectionUtils.isNotEmpty(roleIds)) {
            List<String> keys = roleIds.stream()
                    .map(roleId -> Const.CacheKey.ROLE_PERMS + ":" + roleId)
                    .toList();
            redisCommands.del(keys.toArray(new String[0]));
        }
    }

    private List<Long> getAllMenuIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        ids.add(parentId);
        List<Menu> children = menuRepository.findByParentId(parentId);
        for (Menu child : children) {
            ids.addAll(getAllMenuIds(child.getId()));
        }
        return ids;
    }

    private List<MenuDTO> filterEmptyParentMenus(@NonNull List<MenuDTO> menus) {
        List<MenuDTO> filtered = new ArrayList<>();
        for (MenuDTO menu : menus) {
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                List<MenuDTO> filteredChildren = filterEmptyParentMenus(menu.getChildren());
                menu.setChildren(filteredChildren);
            }

            if ((menu.getChildren() == null || menu.getChildren().isEmpty()) && menu.getPath() != null
                    && !menu.getPath().isEmpty()) {
                filtered.add(menu);
            } else if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                filtered.add(menu);
            }
        }
        return filtered;
    }

    @Transactional(readOnly = true)
    public List<MenuOptionDTO> buildMenuAndPermissionTree() {
        List<Menu> entities = menuRepository.findAll();

        String currentTenantId = TenantContextHolder.getTenantId();
        if (!SysUtil.isSuperTenant(currentTenantId)) {
            Long packageId = tenantRepository.findByTenantId(currentTenantId)
                    .map(Tenant::getPackageId)
                    .orElse(null);

            if (packageId != null) {
                Set<Long> allowedMenuIds = tenantPackageService.getMenuIdsByPackage(packageId);
                Set<Long> allowedPermissionIds = tenantPackageService.getPermissionIdsByPackage(packageId);

                entities = entities.stream()
                        .filter(m -> allowedMenuIds.contains(m.getId()))
                        .toList();

                List<MenuOptionDTO> flatList = entities.stream()
                        .map(entity -> {
                            MenuOptionDTO dto = new MenuOptionDTO();
                            dto.setId(entity.getId());
                            dto.setParentId(entity.getParent() != null ? entity.getParent().getId() : null);
                            dto.setName(entity.getName());
                            dto.setPath(entity.getPath());
                            dto.setIsPermission(false);

                            if (entity.getPermissions() != null) {
                                List<PermissionOptionDTO> pDtos = entity.getPermissions().stream()
                                        .filter(p -> allowedPermissionIds.contains(p.getId()))
                                        .map(p -> {
                                            PermissionOptionDTO pDto = new PermissionOptionDTO();
                                            pDto.setId(p.getId());
                                            pDto.setName(p.getName());
                                            pDto.setIsPermission(true);
                                            return pDto;
                                        }).toList();
                                dto.setPermissions(pDtos);
                            }
                            return dto;
                        }).toList();

                return buildTreeFromFlatList(flatList);
            }
            return Collections.emptyList();
        }

        List<MenuOptionDTO> flatList = entities.stream()
                .map(entity -> {
                    MenuOptionDTO dto = new MenuOptionDTO();
                    dto.setId(entity.getId());
                    dto.setParentId(entity.getParent() != null ? entity.getParent().getId() : null);
                    dto.setName(entity.getName());
                    dto.setPath(entity.getPath());
                    dto.setIsPermission(false);

                    if (entity.getPermissions() != null) {
                        List<PermissionOptionDTO> pDtos = entity.getPermissions().stream()
                                .map(p -> {
                                    PermissionOptionDTO pDto = new PermissionOptionDTO();
                                    pDto.setId(p.getId());
                                    pDto.setName(p.getName());
                                    pDto.setIsPermission(true);
                                    return pDto;
                                }).toList();
                        dto.setPermissions(pDtos);
                    }
                    return dto;
                }).toList();

        return buildTreeFromFlatList(flatList);
    }

    private List<MenuOptionDTO> buildTreeFromFlatList(List<MenuOptionDTO> flatList) {
        Map<Long, MenuOptionDTO> dtoMap = flatList.stream()
                .collect(Collectors.toMap(MenuOptionDTO::getId, dto -> dto));

        List<MenuOptionDTO> rootMenus = new ArrayList<>();

        for (MenuOptionDTO dto : flatList) {
            if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
                if (dto.getChildren() == null) {
                    dto.setChildren(new ArrayList<>());
                }
                for (PermissionOptionDTO perm : dto.getPermissions()) {
                    MenuOptionDTO permNode = new MenuOptionDTO();
                    permNode.setId(perm.getId());
                    permNode.setName("[按钮] " + perm.getName());
                    permNode.setIsPermission(true);
                    dto.getChildren().add(permNode);
                }
            }

            if (dto.getParentId() == null || dto.getParentId() == 0) {
                rootMenus.add(dto);
            } else {
                MenuOptionDTO parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                }
            }
        }
        return filterEmptyParentMenuOptions(rootMenus);
    }

    private List<MenuOptionDTO> filterEmptyParentMenuOptions(@NonNull List<MenuOptionDTO> menus) {
        List<MenuOptionDTO> filtered = new ArrayList<>();
        for (MenuOptionDTO menu : menus) {
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                menu.setChildren(filterEmptyParentMenuOptions(menu.getChildren()));
            }

            boolean hasChildren = menu.getChildren() != null && !menu.getChildren().isEmpty();
            boolean hasPath = menu.getPath() != null && !menu.getPath().isEmpty();
            boolean isButton = Boolean.TRUE.equals(menu.getIsPermission());

            if (isButton || hasPath || hasChildren) {
                filtered.add(menu);
            }
        }
        return filtered;
    }
}
