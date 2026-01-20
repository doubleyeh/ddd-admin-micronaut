package com.mok.common.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Set;

public record CustomUserDetail(Long userId, String username, String password, String tenantId, Set<Long> roleIds,
                               @JsonProperty("superAdmin") boolean isSuperAdmin, Collection<String> authrizes) {

    public CustomUserDetail(Long userId, String username, String password, String tenantId, Set<Long> roleIds, boolean isSuperAdmin, Collection<String> authrizes) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.roleIds = roleIds;
        this.isSuperAdmin = isSuperAdmin;
        this.authrizes = authrizes;
    }
}
