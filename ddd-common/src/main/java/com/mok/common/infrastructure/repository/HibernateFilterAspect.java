package com.mok.common.infrastructure.repository;

import com.mok.common.infrastructure.tenant.TenantContextHolder;
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
        if (!TenantContextHolder.isSuperTenant()) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", TenantContextHolder.getTenantId());
        }
        return context.proceed();
    }
}