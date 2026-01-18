package com.mok.infrastructure.repository;

import com.mok.infrastructure.tenant.TenantContextHolder;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class HibernateFilterAspectTest {

    private EntityManager entityManager;
    private Session session;
    private HibernateFilterAspect aspect;
    private MockedStatic<TenantContextHolder> tenantContextHolderMock;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        session = mock(Session.class);
        aspect = new HibernateFilterAspect(entityManager);
        tenantContextHolderMock = mockStatic(TenantContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextHolderMock.close();
    }

    @Test
    void intercept_SuperTenant_NoFilter() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        Object expectedResult = new Object();
        when(context.proceed()).thenReturn(expectedResult);

        tenantContextHolderMock.when(TenantContextHolder::isSuperTenant).thenReturn(true);

        Object result = aspect.intercept(context);

        assertEquals(expectedResult, result);
        verify(context).proceed();
        verify(entityManager, never()).unwrap(Session.class);
    }

    @Test
    void intercept_NonSuperTenant_EnableFilter() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        Object expectedResult = new Object();
        when(context.proceed()).thenReturn(expectedResult);

        String tenantId = "tenant1";
        tenantContextHolderMock.when(TenantContextHolder::isSuperTenant).thenReturn(false);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(tenantId);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        Filter filter = mock(Filter.class);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);

        Object result = aspect.intercept(context);

        assertEquals(expectedResult, result);
        verify(context).proceed();
        verify(entityManager).unwrap(Session.class);
        verify(session).enableFilter("tenantFilter");
        verify(filter).setParameter("tenantId", tenantId);
    }

    @Test
    void intercept_NonSuperTenant_NullTenantId() throws Exception {
        MethodInvocationContext<Object, Object> context = mock(MethodInvocationContext.class);
        Object expectedResult = new Object();
        when(context.proceed()).thenReturn(expectedResult);

        tenantContextHolderMock.when(TenantContextHolder::isSuperTenant).thenReturn(false);
        tenantContextHolderMock.when(TenantContextHolder::getTenantId).thenReturn(null);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        Filter filter = mock(Filter.class);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);

        Object result = aspect.intercept(context);

        assertEquals(expectedResult, result);
        verify(context).proceed();
        verify(entityManager).unwrap(Session.class);
        verify(session).enableFilter("tenantFilter");
        verify(filter).setParameter("tenantId", null);
    }
}