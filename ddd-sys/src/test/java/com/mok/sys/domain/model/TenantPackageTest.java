package com.mok.sys.domain.model;

import com.mok.common.infrastructure.common.Const;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantPackageTest {

    private TenantPackage createTestPackage(String name, Integer state) {
        try {
            var constructor = TenantPackage.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            TenantPackage pkg = constructor.newInstance();
            setField(pkg, "name", name);
            setField(pkg, "state", state);
            return pkg;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = TenantPackage.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("create")
    class CreateTests {
        @Test
        void create_Success() {
            TenantPackage pkg = TenantPackage.create("Test Package", "Desc");
            assertNotNull(pkg);
            assertEquals("Test Package", pkg.getName());
            assertEquals("Desc", pkg.getDescription());
            assertEquals(Const.TenantPackageState.NORMAL, pkg.getState());
        }
    }

    @Nested
    @DisplayName("updateInfo")
    class UpdateInfoTests {
        @Test
        void updateInfo_Success() {
            TenantPackage pkg = createTestPackage("Old Name", Const.TenantPackageState.NORMAL);
            pkg.updateInfo("New Name", "New Desc");
            assertEquals("New Name", pkg.getName());
            assertEquals("New Desc", pkg.getDescription());
        }
    }

    @Nested
    @DisplayName("State Changes")
    class StateTests {
        @Test
        void enable_ShouldSetStateToNormal() {
            TenantPackage pkg = createTestPackage("Test", Const.TenantPackageState.DISABLED);
            pkg.enable();
            assertEquals(Const.TenantPackageState.NORMAL, pkg.getState());
        }

        @Test
        void disable_ShouldSetStateToDisabled() {
            TenantPackage pkg = createTestPackage("Test", Const.TenantPackageState.NORMAL);
            pkg.disable();
            assertEquals(Const.TenantPackageState.DISABLED, pkg.getState());
        }
    }

    @Nested
    @DisplayName("Association Changes")
    class AssociationTests {
        @Test
        void changeMenus_ShouldUpdateMenus() {
            TenantPackage pkg = createTestPackage("Test", Const.TenantPackageState.NORMAL);
            Set<Menu> menus = new HashSet<>();
            menus.add(new Menu());
            pkg.changeMenus(menus);
            assertEquals(1, pkg.getMenus().size());
        }

        @Test
        void changeMenus_WithNull_ShouldClearMenus() {
            TenantPackage pkg = createTestPackage("Test", Const.TenantPackageState.NORMAL);
            // First add some menus
            Set<Menu> initialMenus = new HashSet<>();
            initialMenus.add(new Menu());
            pkg.changeMenus(initialMenus);
            assertEquals(1, pkg.getMenus().size());

            // Then set to null
            pkg.changeMenus(null);
            assertEquals(0, pkg.getMenus().size());
        }

        @Test
        void changePermissions_ShouldUpdatePermissions() {
            TenantPackage pkg = createTestPackage("Test", Const.TenantPackageState.NORMAL);
            Set<Permission> permissions = new HashSet<>();
            permissions.add(new Permission());
            pkg.changePermissions(permissions);
            assertEquals(1, pkg.getPermissions().size());
        }

        @Test
        void changePermissions_WithNull_ShouldClearPermissions() {
            TenantPackage pkg = createTestPackage("Test", Const.TenantPackageState.NORMAL);
            // First add some permissions
            Set<Permission> initialPermissions = new HashSet<>();
            initialPermissions.add(new Permission());
            pkg.changePermissions(initialPermissions);
            assertEquals(1, pkg.getPermissions().size());

            // Then set to null
            pkg.changePermissions(null);
            assertEquals(0, pkg.getPermissions().size());
        }
    }
}
