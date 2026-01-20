package com.mok.sys.domain.model;

import com.mok.common.domain.TenantBaseEntity;
import com.mok.common.infrastructure.common.Const;
import com.mok.common.infrastructure.repository.AuditEntityListener;

import io.micronaut.core.annotation.Introspected;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sys_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @BatchSize(size = 20)
    private Set<Role> roles;

    public static User create(@NonNull String username, @NonNull String password, String nickname, boolean isTenantAdmin) {
        User user = new User();
        user.username = username;
        user.password = password;
        user.nickname = nickname;
        user.isTenantAdmin = isTenantAdmin;
        user.state = Const.UserState.NORMAL;
        return user;
    }

    public void assignTenant(String tenantId) {
        if (this.getTenantId() == null) {
            this.setTenantId(tenantId);
        }
    }

    public void updateInfo(String nickname, Set<Role> roles) {
        this.nickname = nickname;
        this.roles = roles;
    }

    public void changePassword(@NonNull String newPassword) {
        this.password = newPassword;
    }

    public void disable() {
        this.state = Const.UserState.DISABLED;
    }

    public void enable() {
        this.state = Const.UserState.NORMAL;
    }

    public void changeRoles(Set<Role> newRoles) {
        this.roles = newRoles;
    }
}
