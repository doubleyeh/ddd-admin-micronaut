package com.mok.common.infrastructure.repository.filter;

import com.mok.common.infrastructure.common.Const;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.multitenancy.exceptions.TenantNotFoundException;
import io.micronaut.multitenancy.tenantresolver.TenantResolver;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import java.io.Serializable;

@Singleton
@TenantFilter
public class TenantFilterInterceptor implements MethodInterceptor<Object, Object> {

    private final EntityManager entityManager;
    private final TenantResolver tenantResolver;

    public TenantFilterInterceptor(EntityManager entityManager, TenantResolver tenantResolver) {
        this.entityManager = entityManager;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Serializable tenantId = null;
        try {
            tenantId = tenantResolver.resolveTenantId();
        } catch (TenantNotFoundException e) {
            // ignore
        }

        if (tenantId != null && !Const.SUPER_TENANT_ID.equals(tenantId.toString())) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId.toString());
        }

        return context.proceed();
    }
}
