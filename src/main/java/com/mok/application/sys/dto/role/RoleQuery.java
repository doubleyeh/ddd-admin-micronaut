package com.mok.application.sys.dto.role;

import com.mok.domain.sys.model.Role;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Serdeable
public class RoleQuery {
    private String name;
    private String code;
    private Integer state;

    public PredicateSpecification<Role> toPredicate() {
        return (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if (code != null && !code.isEmpty()) {
                predicates.add(cb.equal(root.get("code"), code));
            }
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}