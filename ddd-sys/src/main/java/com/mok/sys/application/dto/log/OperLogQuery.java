package com.mok.sys.application.dto.log;

import com.mok.sys.domain.model.OperLog;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Serdeable
public class OperLogQuery {

    private String title;
    private String operName;
    private Integer status;

    public PredicateSpecification<OperLog> toPredicate() {
        return (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (title != null && !title.isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title + "%"));
            }
            if (operName != null && !operName.isEmpty()) {
                predicates.add(cb.like(root.get("operName"), "%" + operName + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
