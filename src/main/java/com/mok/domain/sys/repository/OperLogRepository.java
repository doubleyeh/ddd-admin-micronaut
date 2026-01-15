package com.mok.domain.sys.repository;

import com.mok.domain.sys.model.OperLog;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;

@Repository
public interface OperLogRepository extends PageableRepository<OperLog, Long>, JpaSpecificationExecutor<OperLog> {

    Page<OperLog> findAll(@Nullable PredicateSpecification<OperLog> spec, Pageable pageable);
}