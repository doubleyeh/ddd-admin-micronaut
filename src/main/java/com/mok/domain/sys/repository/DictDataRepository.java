package com.mok.domain.sys.repository;


import com.mok.domain.sys.model.DictData;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DictDataRepository extends JpaRepository<DictData, Long> {
    List<DictData> findByTypeCodeOrderBySortAsc(String typeCode);

    @Transactional
    void deleteByTypeCode(String typeCode);
}
