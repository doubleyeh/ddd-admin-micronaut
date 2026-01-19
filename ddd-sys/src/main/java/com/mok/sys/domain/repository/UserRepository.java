package com.mok.sys.domain.repository;

import com.mok.sys.application.dto.user.UserDTO;
import com.mok.sys.domain.model.User;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;

import java.util.Optional;

@Repository
public interface UserRepository extends PageableRepository<User, Long>, JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByTenantIdAndUsername(String tenantId, String username);

    @Query(value = """
            SELECT u.id as id, u.username as username, u.nickname as nickname,
                   u.state as state, u.createTime as createTime, u.tenantId as tenantId,
                   t.name as tenantName
            FROM User u LEFT JOIN Tenant t ON u.tenantId = t.tenantId
            """,
            countQuery = "SELECT count(u) FROM User u")
    Page<UserDTO> findUserPage(PredicateSpecification<User> spec, Pageable pageable);
}
