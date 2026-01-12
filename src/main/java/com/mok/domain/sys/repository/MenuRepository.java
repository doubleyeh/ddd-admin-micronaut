package com.mok.domain.sys.repository;

import com.mok.domain.sys.model.Menu;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByParentId(Long parentId);

    @Query(value = "SELECT DISTINCT role_id FROM sys_role_menu WHERE menu_id IN :menuIds", nativeQuery = true)
    List<Long> findRoleIdsByMenuIds(List<Long> menuIds);

    @Query(value = "DELETE FROM sys_role_menu WHERE menu_id IN :menuIds", nativeQuery = true)
    void deleteRoleMenuByMenuIds(List<Long> menuIds);
}