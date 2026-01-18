package com.mok.infrastructure.common;

import com.mok.domain.common.BaseEntity;
import io.micronaut.security.utils.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditEntityListenerTest {

    private SecurityService securityService;
    private AuditEntityListener auditEntityListener;

    @BeforeEach
    void setUp() {
        securityService = mock(SecurityService.class);
        auditEntityListener = new AuditEntityListener(securityService);
    }

    @Test
    void setCreatedBy_WithAuthenticatedUser() {
        BaseEntity entity = mock(BaseEntity.class);
        when(securityService.username()).thenReturn(Optional.of("testUser"));

        auditEntityListener.setCreatedBy(entity);

        verify(entity).setCreateBy("testUser");
        verify(entity).setUpdateBy("testUser");
    }

    @Test
    void setCreatedBy_WithoutAuthenticatedUser() {
        BaseEntity entity = mock(BaseEntity.class);
        when(securityService.username()).thenReturn(Optional.empty());

        auditEntityListener.setCreatedBy(entity);

        verify(entity).setCreateBy("system");
        verify(entity).setUpdateBy("system");
    }

    @Test
    void setCreatedBy_NonBaseEntity() {
        Object entity = new Object();

        auditEntityListener.setCreatedBy(entity);

        verifyNoInteractions(securityService);
    }

    @Test
    void setUpdatedBy_WithAuthenticatedUser() {
        BaseEntity entity = mock(BaseEntity.class);
        when(securityService.username()).thenReturn(Optional.of("testUser"));

        auditEntityListener.setUpdatedBy(entity);

        verify(entity).setUpdateBy("testUser");
    }

    @Test
    void setUpdatedBy_WithoutAuthenticatedUser() {
        BaseEntity entity = mock(BaseEntity.class);
        when(securityService.username()).thenReturn(Optional.empty());

        auditEntityListener.setUpdatedBy(entity);

        verify(entity).setUpdateBy("system");
    }

    @Test
    void setUpdatedBy_NonBaseEntity() {
        Object entity = new Object();

        auditEntityListener.setUpdatedBy(entity);

        verifyNoInteractions(securityService);
    }
}
