package com.mok.domain.sys.repository;

import com.mok.application.sys.dto.tenant.TenantDTO;
import com.mok.domain.sys.model.Tenant;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends PageableRepository<Tenant, Long>, JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantId(String tenantId);

    long countByPackageId(Long packageId);

    List<Tenant> findByNameContainsIgnoreCaseAndState(String name, Integer state);

    List<Tenant> findByState(Integer state);

    @Query(value = """
            SELECT t.id as id, t.tenantId as tenantId, t.name as name,
                   t.contactPerson as contactPerson, t.contactPhone as contactPhone,
                   t.state as state, t.packageId as packageId, tp.name as packageName
            FROM Tenant t LEFT JOIN TenantPackage tp ON t.packageId = tp.id
            """,
            countQuery = "SELECT count(t) FROM Tenant t")
    Page<TenantDTO> findTenantPage(PredicateSpecification<Tenant> spec, Pageable pageable);
}