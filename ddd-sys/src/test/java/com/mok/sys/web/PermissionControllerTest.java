package com.mok.sys.web;

import com.mok.sys.application.dto.permission.PermissionDTO;
import com.mok.sys.application.dto.permission.PermissionQuery;
import com.mok.sys.application.service.PermissionService;
import com.mok.common.web.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PermissionControllerTest {

    private PermissionService permissionService;
    private PermissionController permissionController;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        permissionController = new PermissionController(permissionService);
    }

    @Test
    void findByMenuId() {
        Long menuId = 1L;
        List<PermissionDTO> permissions = Collections.singletonList(new PermissionDTO());
        when(permissionService.findAll(any(PermissionQuery.class))).thenReturn(permissions);

        RestResponse<List<PermissionDTO>> response = permissionController.findByMenuId(menuId);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(permissions, response.getData());

        verify(permissionService).findAll(argThat(query -> query.getMenuId().equals(menuId)));
    }

    @Test
    void save() {
        PermissionDTO dto = new PermissionDTO();
        PermissionDTO savedDto = new PermissionDTO();
        savedDto.setId(1L);
        when(permissionService.createPermission(dto)).thenReturn(savedDto);

        RestResponse<PermissionDTO> response = permissionController.save(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(savedDto, response.getData());

        verify(permissionService).createPermission(dto);
    }

    @Test
    void update() {
        Long id = 1L;
        PermissionDTO dto = new PermissionDTO();
        PermissionDTO updatedDto = new PermissionDTO();
        updatedDto.setId(id);
        when(permissionService.updatePermission(any(PermissionDTO.class))).thenReturn(updatedDto);

        RestResponse<PermissionDTO> response = permissionController.update(id, dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());
        assertEquals(id, dto.getId());

        verify(permissionService).updatePermission(dto);
    }

    @Test
    void delete() {
        Long id = 1L;
        doNothing().when(permissionService).deleteById(id);

        RestResponse<Void> response = permissionController.delete(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(permissionService).deleteById(id);
    }
}