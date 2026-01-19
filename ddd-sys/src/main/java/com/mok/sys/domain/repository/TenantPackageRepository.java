package com.mok.sys.domain.repository;


import com.mok.sys.domain.model.TenantPackage;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;

import java.util.List;

@Repository
public interface TenantPackageRepository extends PageableRepository<TenantPackage, Long>, JpaRepository<TenantPackage, Long> {

    List<TenantPackage> findByNameContainsIgnoreCaseAndState(String name, Integer state);

    List<TenantPackage> findByState(Integer state);

    Page<TenantPackage> findAll(@Nullable PredicateSpecification<TenantPackage> spec, Pageable pageable);
}
