package com.mok.sys.domain.repository;

import com.mok.sys.domain.model.Menu;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByParentId(Long parentId);

    @Query(value = "SELECT DISTINCT role_id FROM sys_role_menu WHERE menu_id IN :menuIds", nativeQuery = true)
    List<Long> findRoleIdsByMenuIds(List<Long> menuIds);

    @Transactional
    @Query(value = "DELETE FROM sys_role_menu WHERE menu_id IN :menuIds", nativeQuery = true)
    void deleteRoleMenuByMenuIds(List<Long> menuIds);

    @Transactional
    @Query("DELETE FROM com.mok.ddd.domain.sys.model.Menu m WHERE m.id IN :ids")
    void deleteAllById(Collection<Long> ids);

    List<Menu> findByIdIn(Collection<Long> ids);
}
