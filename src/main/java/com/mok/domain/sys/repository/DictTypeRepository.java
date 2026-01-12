package com.mok.domain.sys.repository;


import com.mok.domain.sys.model.DictType;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface DictTypeRepository extends JpaRepository<DictType, Long> {
    Optional<DictType> findByCode(String code);
    boolean existsByCode(String code);
}
