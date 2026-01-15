package com.mok.domain.common;

import com.mok.infrastructure.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

@DisplayName("TenantBaseEntity 领域模型测试")
class TenantBaseEntityTest {

    private MockedStatic<TenantContextHolder> mockedTenantContext;

    @BeforeEach
    void setUp() {
        mockedTenantContext = mockStatic(TenantContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        mockedTenantContext.close();
    }

    static class TestTenantEntity extends TenantBaseEntity {
        public void triggerPrePersist() {
            super.onCreate();
        }
        
        public void publicSetTenantId(String tenantId) {
            super.setTenantId(tenantId);
        }
    }

    @Test
    @DisplayName("prePersist - 自动填充租户ID")
    void prePersist_AutoFillTenantId() {
        String tenantId = "tenant-123";
        mockedTenantContext.when(TenantContextHolder::getTenantId).thenReturn(tenantId);

        TestTenantEntity entity = new TestTenantEntity();
        entity.triggerPrePersist();

        assertEquals(tenantId, entity.getTenantId());
    }

    @Test
    @DisplayName("prePersist - 已有租户ID不覆盖")
    void prePersist_ExistingTenantId() {
        String tenantId = "tenant-123";
        String existingTenantId = "existing-tenant";
        mockedTenantContext.when(TenantContextHolder::getTenantId).thenReturn(tenantId);

        TestTenantEntity entity = new TestTenantEntity();
        entity.publicSetTenantId(existingTenantId);
        entity.triggerPrePersist();

        assertEquals(existingTenantId, entity.getTenantId());
    }
}
