package com.mok.domain.sys.model;

import com.mok.domain.common.TenantBaseEntity;
import com.mok.infrastructure.common.AuditEntityListener;
import io.micronaut.core.annotation.Introspected;
import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "sys_user")
@EntityListeners(AuditEntityListener.class)
@Getter
@Setter
@Introspected
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends TenantBaseEntity {
    private String username;
    private String password;
    private String nickname;
    private Integer state;

    @Column(name = "is_tenant_admin")
    private Boolean isTenantAdmin = false;

    //@ManyToMany(fetch = FetchType.LAZY)
    //@JoinTable(
    //        name = "sys_user_role",
    //        joinColumns = @JoinColumn(name = "user_id"),
    //        inverseJoinColumns = @JoinColumn(name = "role_id")
    //)
    //private Set<Role> roles;

    public static User create(String username, String password, String nickname, boolean isTenantAdmin) {
        User user = new User();
        user.username = username;
        user.password = password;
        user.nickname = nickname;
        user.isTenantAdmin = isTenantAdmin;
        user.state = 1;
        return user;
    }
}