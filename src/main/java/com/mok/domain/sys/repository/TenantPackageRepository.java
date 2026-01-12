package com.mok.domain.sys.repository;


import com.mok.domain.sys.model.TenantPackage;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

@Repository
public interface TenantPackageRepository extends JpaRepository<TenantPackage, Long> {
}