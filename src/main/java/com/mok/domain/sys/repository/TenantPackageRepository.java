package com.mok.domain.sys.repository;


import com.mok.domain.sys.model.Tenant;
import com.mok.domain.sys.model.TenantPackage;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface TenantPackageRepository extends JpaRepository<TenantPackage, Long> {

    List<TenantPackage> findByNameContainsIgnoreCaseAndState(String name, Integer state);

    List<TenantPackage> findByState(Integer state);
}