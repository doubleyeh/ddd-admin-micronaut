package com.mok.infrastructure.tenant;

import com.mok.infrastructure.common.Const;

public class TenantContext {
    public static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

    public static String getTenantId() {
        return TENANT_ID.isBound() ? TENANT_ID.get() : Const.SUPER_TENANT_ID;
    }

    public static boolean isSuperTenant() {
        return Const.SUPER_TENANT_ID.equals(getTenantId());
    }
}