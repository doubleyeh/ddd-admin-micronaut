package com.mok.application.sys.mapper;

import com.mok.application.sys.dto.permission.PermissionDTO;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.model.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PermissionMapperTest {

    private final PermissionMapper permissionMapper = new PermissionMapperImpl();

    @Test
    void toDto() {
        Menu menu = Menu.create(null, "Test Menu", "/test", null, null, 1, false);

        Permission entity = Permission.create("Read Permission", "perm:read", null, null, null, menu);

        PermissionDTO dto = permissionMapper.toDto(entity);

        assertNotNull(dto);
        assertEquals("perm:read", dto.getCode());
        assertEquals("Read Permission", dto.getName());
        assertNull(dto.getMenuId()); // menu.id is null
    }

    @Test
    void toDtoList() {
        Permission entity1 = Permission.create("Perm1", "code1", null, null, null, null);
        Permission entity2 = Permission.create("Perm2", "code2", null, null, null, null);

        List<Permission> list = List.of(entity1, entity2);
        List<PermissionDTO> dtos = permissionMapper.toDtoList(list);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("code1", dtos.get(0).getCode());
        assertEquals("code2", dtos.get(1).getCode());
    }

    @Test
    void toDtoList_NullList() {
        List<PermissionDTO> dtos = permissionMapper.toDtoList(null);

        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }
}