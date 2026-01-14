package com.mok.application.sys.dto.permission;

import com.mok.domain.sys.model.Permission;
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
public class PermissionQuery {

    private Long menuId;

    public PredicateSpecification<Permission> toPredicate() {
        return (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (menuId != null) {
                predicates.add(cb.equal(root.get("menu").get("id"), menuId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}