package com.mok.sys.domain.model;

import com.mok.common.domain.TenantBaseEntity;
import io.micronaut.core.annotation.Introspected;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sys_login_log")
@Introspected
public class LoginLog extends TenantBaseEntity {

    private String username;

    private String ipAddress;

    private String status;

    private String message;

    public static LoginLog create(String username, String ipAddress, String status, String message) {
        LoginLog log = new LoginLog();
        log.username = username;
        log.ipAddress = ipAddress;
        log.status = status;
        log.message = message;
        return log;
    }

    public void assignTenant(String tenantId) {
        if (this.getTenantId() == null) {
            this.setTenantId(tenantId);
        }
    }
}
