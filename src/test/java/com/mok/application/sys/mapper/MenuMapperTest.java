package com.mok.application.sys.mapper;

import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.domain.sys.model.Menu;
import com.mok.domain.sys.model.Permission;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MenuMapperTest {

    private final MenuMapper menuMapper = new MenuMapperImpl();

    @Test
    void toDto() {
        Menu parent = Menu.create(null, "Parent", "/parent", null, null, 1, false);

        Menu entity = Menu.create(parent, "Test Menu", "/test", null, null, 1, false);

        MenuDTO dto = menuMapper.toDto(entity);

        assertNotNull(dto);
        assertEquals("Test Menu", dto.getName());
        assertEquals("/test", dto.getPath());
        assertNull(dto.getParentId()); // parent.id is null
        assertEquals(Collections.emptySet(), dto.getPermissionIds()); // no permissions set
    }

    @Test
    void toDtoList() {
        Menu entity1 = Menu.create(null, "Menu1", "/menu1", null, null, 1, false);

        Menu entity2 = Menu.create(null, "Menu2", "/menu2", null, null, 1, false);

        List<Menu> list = List.of(entity1, entity2);
        List<MenuDTO> dtos = menuMapper.toDtoList(list);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Menu1", dtos.get(0).getName());
        assertEquals("Menu2", dtos.get(1).getName());
    }

    @Test
    void toDtoList_NullList() {
        List<MenuDTO> dtos = menuMapper.toDtoList(null);

        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }



    @Test
    void permsToIds_NullPermissions() {
        Set<Long> ids = menuMapper.permsToIds(null);

        assertNotNull(ids);
        assertTrue(ids.isEmpty());
    }
}