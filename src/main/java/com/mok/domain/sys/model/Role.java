package com.mok.domain.sys.model;

import com.mok.domain.common.TenantBaseEntity;
import com.mok.infrastructure.common.Const;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sys_role")
@Getter
@Introspected
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends TenantBaseEntity {
    private String name;
    private String code;
    private String description;
    private Integer sort;

    /**
     * 状态 (1:正常, 0:禁用)
     */
    private Integer state;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sys_role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sys_role_menu",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id")
    )
    private Set<Menu> menus;

    public static Role create(@NonNull String name, @NonNull String code, String description, Integer sort) {
        Role role = new Role();
        role.name = name;
        role.code = code;
        role.description = description;
        role.sort = sort;
        role.state = Const.RoleState.NORMAL;
        return role;
    }

    public void updateInfo(String name, String code, String description, Integer sort) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.sort = sort;
    }

    public void disable() {
        if (this.state.equals(Const.RoleState.DISABLED)) {
            return;
        }
        this.state = Const.RoleState.DISABLED;
    }

    public void enable() {
        if (this.state.equals(Const.RoleState.NORMAL)) {
            return;
        }
        this.state = Const.RoleState.NORMAL;
    }

    public void changePermissions(Set<Permission> newPermissions) {
        if (this.permissions == null) {
            this.permissions = new HashSet<>();
        } else {
            this.permissions.clear();
        }
        if (newPermissions != null) {
            this.permissions.addAll(newPermissions);
        }
    }

    public void changeMenus(Set<Menu> newMenus) {
        if (this.menus == null) {
            this.menus = new HashSet<>();
        } else {
            this.menus.clear();
        }
        if (newMenus != null) {
            this.menus.addAll(newMenus);
        }
    }
}
