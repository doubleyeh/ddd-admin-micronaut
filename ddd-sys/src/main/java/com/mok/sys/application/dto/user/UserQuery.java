package com.mok.sys.application.dto.user;

import com.mok.sys.domain.model.Role;
import com.mok.sys.domain.model.User;
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
public class UserQuery {
    private String username;
    private String nickname;
    private Integer state;
    private String tenantId;

    private Long roleId;

    public PredicateSpecification<User> toPredicate() {
        return (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null && !username.isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (nickname != null && !nickname.isEmpty()) {
                predicates.add(cb.like(root.get("nickname"), "%" + nickname + "%"));
            }
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (tenantId != null && !tenantId.isEmpty()) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            if (roleId != null) {
                Join<User, Role> roles = root.join("roles");
                predicates.add(cb.equal(roles.get("id"), roleId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
