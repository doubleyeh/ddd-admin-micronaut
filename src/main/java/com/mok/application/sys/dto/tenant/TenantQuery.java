package com.mok.application.sys.dto.tenant;

import com.mok.domain.sys.model.Tenant;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
public class TenantQuery {
    private String tenantId;
    private String name;
    private Integer state;

    public PredicateSpecification<Tenant> toPredicate() {
        return (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null && !tenantId.isEmpty()) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
