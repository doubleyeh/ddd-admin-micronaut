package com.mok.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Collection;
import java.util.Set;

@Getter
public class CustomUserDetail {

    private final Long userId;
    private final String username;
    private final String password;
    private final String tenantId;
    private final Set<Long> roleIds;
    private final Collection<String> roles;
    @JsonProperty("superAdmin")
    private final boolean isSuperAdmin;

    public CustomUserDetail(Long userId, String username, String password, String tenantId, Set<Long> roleIds, boolean isSuperAdmin, Collection<String> roles) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.roleIds = roleIds;
        this.isSuperAdmin = isSuperAdmin;
        this.roles = roles;
    }
}
