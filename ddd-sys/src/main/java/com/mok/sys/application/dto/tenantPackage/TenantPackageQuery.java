package com.mok.sys.application.dto.tenantPackage;

import com.mok.sys.domain.model.TenantPackage;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Serdeable
public class TenantPackageQuery {
    private String name;
    private Integer state;

    public PredicateSpecification<TenantPackage> toPredicate() {
        return (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
