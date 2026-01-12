package com.mok.infrastructure.repository;

import com.mok.infrastructure.tenant.TenantContext;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;

@Singleton
public class HibernateFilterAspect implements MethodInterceptor<Object, Object> {
    private final EntityManager entityManager;

    public HibernateFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        if (!TenantContext.isSuperTenant()) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", TenantContext.getTenantId());
        }
        return context.proceed();
    }
}