package com.mok.domain.sys.repository;

import com.mok.domain.sys.model.Tenant;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantId(String tenantId);

    long countByPackageId(Long packageId);
}