package com.mok.sys.domain.repository;


import com.mok.sys.domain.model.DictType;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends PageableRepository<DictType, Long>, JpaRepository<DictType, Long> {
    Optional<DictType> findByCode(String code);

    boolean existsByCode(String code);

    Page<DictType> findAll(@Nullable PredicateSpecification<DictType> spec, Pageable pageable);
}
