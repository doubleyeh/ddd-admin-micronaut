package com.mok.domain.sys.repository;


import com.mok.domain.sys.model.LoginLog;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}
