package com.mok.sys.domain.repository;


import com.mok.sys.domain.model.Permission;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PermissionRepository extends PageableRepository<Permission, Long>, JpaRepository<Permission, Long> {

    @Transactional
    @Query("update com.mok.ddd.domain.sys.model.Permission p set p.menu.id = ?1 where p.id in ?2")
    void bindMenuId(Long menuId, Set<Long> ids);

    @Query(value = "SELECT DISTINCT role_id FROM sys_role_permission WHERE permission_id = :id", nativeQuery = true)
    List<Long> findRoleIdsByPermissionId(Long id);

    @Transactional
    @Query(value = "DELETE FROM sys_role_permission WHERE permission_id = :id", nativeQuery = true)
    void deleteRolePermissionsByPermissionId(Long id);

    @Transactional
    @Query(value = "DELETE FROM sys_role_permission WHERE permission_id IN (SELECT id FROM sys_permission WHERE menu_id IN :menuIds)", nativeQuery = true)
    void deleteRolePermissionsByMenuIds(List<Long> menuIds);

    @Transactional
    @Query("DELETE FROM com.mok.ddd.domain.sys.model.Permission p WHERE p.menu.id IN :menuIds")
    void deleteByMenuIds(List<Long> menuIds);

    @Query(value = "SELECT p.code FROM sys_permission p " +
            "JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = :roleId", nativeQuery = true)
    List<String> findCodesByRoleId(Long roleId);

    @Query("SELECT p FROM com.mok.ddd.domain.sys.model.Permission p WHERE p.id IN :ids")
    List<Permission> findAllById(Collection<Long> ids);

    Page<Permission> findAll(@Nullable PredicateSpecification<Permission> spec, Pageable pageable);

    List<Permission> findAll(@Nullable PredicateSpecification<Permission> spec);
}
