package com.mok.domain.sys.repository;


import com.mok.domain.sys.model.OperLog;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

@Repository
public interface OperLogRepository extends JpaRepository<OperLog, Long> {
}
