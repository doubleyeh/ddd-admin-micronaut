package com.mok.application.sys.dto.log;

import com.mok.domain.sys.model.LoginLog;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Serdeable
public class LoginLogQuery {
    private String username;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public PredicateSpecification<LoginLog> toPredicate() {
        return (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null && !username.isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            // 时间范围过滤
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}