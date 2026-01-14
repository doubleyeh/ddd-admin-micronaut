package com.mok.domain.sys.repository;


import com.mok.application.sys.dto.log.LoginLogDTO;
import com.mok.domain.sys.model.LoginLog;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;

@Repository
public interface LoginLogRepository extends PageableRepository<LoginLog, Long>, JpaRepository<LoginLog, Long> {

    @Query(value = """
            SELECT l.id as id, l.username as username, l.ipAddress as ipAddress, 
                   l.status as status, l.message as message, l.tenantId as tenantId, 
                   l.createTime as createTime, t.name as tenantName 
            FROM LoginLog l LEFT JOIN Tenant t ON l.tenantId = t.tenantId
            """,
            countQuery = "SELECT count(l) FROM LoginLog l")
    Page<LoginLogDTO> findLoginLogPage(PredicateSpecification<LoginLog> spec, Pageable pageable);
}