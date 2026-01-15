package com.mok.web.sys;

import com.mok.application.sys.dto.menu.MenuDTO;
import com.mok.application.sys.dto.menu.MenuOptionDTO;
import com.mok.application.sys.service.MenuService;
import com.mok.web.common.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuControllerTest {

    private MenuService menuService;
    private MenuController menuController;

    @BeforeEach
    void setUp() {
        menuService = mock(MenuService.class);
        menuController = new MenuController(menuService);
    }

    @Test
    void getTree() {
        List<MenuDTO> menuList = Collections.singletonList(new MenuDTO());
        when(menuService.findAll()).thenReturn(menuList);
        when(menuService.buildMenuTree(menuList)).thenReturn(menuList);

        RestResponse<List<MenuDTO>> response = menuController.getTree();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(menuList, response.getData());

        verify(menuService).findAll();
        verify(menuService).buildMenuTree(menuList);
    }

    @Test
    void save() {
        MenuDTO dto = new MenuDTO();
        dto.setName("New Menu");
        MenuDTO createdDto = new MenuDTO();
        createdDto.setId(1L);
        createdDto.setName("New Menu");

        when(menuService.createMenu(dto)).thenReturn(createdDto);

        RestResponse<MenuDTO> response = menuController.save(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(createdDto, response.getData());

        verify(menuService).createMenu(dto);
    }

    @Test
    void update() {
        Long id = 1L;
        MenuDTO dto = new MenuDTO();
        dto.setName("Updated Menu");
        MenuDTO updatedDto = new MenuDTO();
        updatedDto.setId(id);
        updatedDto.setName("Updated Menu");

        when(menuService.updateMenu(dto)).thenReturn(updatedDto);

        RestResponse<MenuDTO> response = menuController.update(id, dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());
        assertEquals(id, dto.getId());

        verify(menuService).updateMenu(dto);
    }

    @Test
    void delete() {
        Long id = 1L;
        doNothing().when(menuService).deleteById(id);

        RestResponse<Void> response = menuController.delete(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(menuService).deleteById(id);
    }

    @Test
    void getMenuTreeOptions() {
        List<MenuOptionDTO> options = Collections.singletonList(new MenuOptionDTO());
        when(menuService.buildMenuAndPermissionTree()).thenReturn(options);

        RestResponse<List<MenuOptionDTO>> response = menuController.getMenuTreeOptions();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(options, response.getData());

        verify(menuService).buildMenuAndPermissionTree();
    }
}
