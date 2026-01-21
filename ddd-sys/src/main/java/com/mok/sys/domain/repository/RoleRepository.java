package com.mok.sys.domain.repository;


import com.mok.common.infrastructure.repository.filter.TenantFilter;
import com.mok.sys.application.dto.role.RoleDTO;
import com.mok.sys.domain.model.Role;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;

import java.util.Collection;
import java.util.List;

@Repository
@TenantFilter
public interface RoleRepository extends PageableRepository<Role, Long>, JpaRepository<Role, Long> {

    @Query("select count(u) > 0 from com.mok.ddd.domain.sys.model.User u join u.roles r where r.id = :roleId")
    boolean existsUserAssociatedWithRole(Long roleId);

    @Query("select count(1) from sys_user_role where role_id = :roleId")
    boolean existsByRolesId(Long roleId);

    List<Role> findByIdIn(Collection<Long> ids);

    @Query(value = """
            SELECT r.id as id, r.name as name, r.code as code,
                   r.description as description, r.sort as sort,
                   r.state as state, r.tenantId as tenantId, r.createTime as createTime,
                   t.name as tenantName
            FROM Role r LEFT JOIN Tenant t ON r.tenantId = t.tenantId
            """,
            countQuery = "SELECT count(r) FROM Role r")
    Page<RoleDTO> findRolePage(PredicateSpecification<Role> spec, Pageable pageable);

    List<Role> findAll(PredicateSpecification<Role> spec);
}
