package com.mok.sys.domain.model;

import com.mok.common.application.exception.BizException;
import com.mok.common.domain.BaseEntity;
import com.mok.sys.domain.repository.TenantRepository;
import com.mok.common.infrastructure.common.Const;
import com.mok.common.infrastructure.util.SysUtil;
import io.micronaut.core.annotation.Introspected;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "sys_tenant")
@Getter
@Setter
@Introspected
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String tenantId;
    @Column(nullable = false)
    private String name;
    private String contactPerson;
    private String contactPhone;

    /**
     * 状态 (1:正常, 0:禁用)
     */
    private Integer state;

    private Long packageId;

    public static Tenant create(@NonNull String name, String contactPerson, String contactPhone, @NonNull Long packageId, @NonNull TenantRepository tenantRepository) {
        Tenant tenant = new Tenant();
        tenant.name = name;
        tenant.contactPerson = contactPerson;
        tenant.contactPhone = contactPhone;
        tenant.packageId = packageId;
        tenant.state = Const.TenantState.NORMAL;

        int maxRetry = 5;
        int attempt = 0;
        String newTenantId;
        final String ALPHANUMERIC_UPPER = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();

        do {
            if (attempt++ >= maxRetry) {
                throw new BizException("生成唯一租户编码失败，请重试");
            }

            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHANUMERIC_UPPER.charAt(random.nextInt(ALPHANUMERIC_UPPER.length())));
            }
            newTenantId = sb.toString();

        } while (tenantRepository.findByTenantId(newTenantId).isPresent());

        tenant.tenantId = newTenantId;
        return tenant;
    }

    public void disable() {
        if (SysUtil.isSuperTenant(this.tenantId)) {
            throw new BizException("无法对该租户进行操作");
        }
        if (this.state.equals(Const.TenantState.DISABLED)) {
            return;
        }
        this.state = Const.TenantState.DISABLED;
    }

    public void enable() {
        if (this.state.equals(Const.TenantState.NORMAL)) {
            return;
        }
        this.state = Const.TenantState.NORMAL;
    }

    public void changePackage(Long newPackageId) {
        if (SysUtil.isSuperTenant(this.tenantId)) {
            return;
        }
        if (newPackageId == null) {
            throw new BizException("套餐不能为空");
        }
        this.packageId = newPackageId;
    }

    public void updateInfo(String name, String contactPerson, String contactPhone) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.contactPhone = contactPhone;
    }
}
