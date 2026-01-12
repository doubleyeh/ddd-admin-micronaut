package com.mok.domain.sys.repository;

import com.mok.domain.sys.model.User;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByTenantIdAndUsername(String tenantId, String username);
}