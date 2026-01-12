package com.mok.domain.sys.repository;


import com.mok.domain.sys.model.Permission;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query("update com.mok.ddd.domain.sys.model.Permission p set p.menu.id = ?1 where p.id in ?2")
    void bindMenuId(Long menuId, Set<Long> ids);

    @Query(value = "SELECT DISTINCT role_id FROM sys_role_permission WHERE permission_id = :id", nativeQuery = true)
    List<Long> findRoleIdsByPermissionId(Long id);

    @Query(value = "DELETE FROM sys_role_permission WHERE permission_id = :id", nativeQuery = true)
    void deleteRolePermissionsByPermissionId(Long id);

    @Query(value = "DELETE FROM sys_role_permission WHERE permission_id IN (SELECT id FROM sys_permission WHERE menu_id IN :menuIds)", nativeQuery = true)
    void deleteRolePermissionsByMenuIds(List<Long> menuIds);

    @Query("DELETE FROM com.mok.ddd.domain.sys.model.Permission p WHERE p.menu.id IN :menuIds")
    void deleteByMenuIds(List<Long> menuIds);

    @Query(value = "SELECT p.code FROM sys_permission p " +
            "JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = :roleId", nativeQuery = true)
    List<String> findCodesByRoleId(Long roleId);
}