package com.mok.web.sys;

import com.mok.application.sys.dto.role.*;
import com.mok.application.sys.service.RoleService;
import com.mok.web.common.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RoleControllerTest {

    private RoleService roleService;
    private RoleController roleController;

    @BeforeEach
    void setUp() {
        roleService = mock(RoleService.class);
        roleController = new RoleController(roleService);
    }

    @Test
    void findPage() {
        RoleQuery query = new RoleQuery();
        Pageable pageable = Pageable.from(0, 10);
        Page<RoleDTO> page = Page.of(Collections.emptyList(), pageable, 0L);

        when(roleService.findPage(query, pageable)).thenReturn(page);

        RestResponse<Page<RoleDTO>> response = roleController.findPage(query, pageable);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(page, response.getData());

        verify(roleService).findPage(query, pageable);
    }

    @Test
    void getById() {
        Long id = 1L;
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(id);

        when(roleService.getById(id)).thenReturn(roleDTO);

        RestResponse<RoleDTO> response = roleController.getById(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(roleDTO, response.getData());

        verify(roleService).getById(id);
    }

    @Test
    void save() {
        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setName("New Role");
        RoleDTO createdDto = new RoleDTO();
        createdDto.setId(1L);
        createdDto.setName("New Role");

        when(roleService.createRole(dto)).thenReturn(createdDto);

        RestResponse<RoleDTO> response = roleController.save(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(createdDto, response.getData());

        verify(roleService).createRole(dto);
    }

    @Test
    void update() {
        Long id = 1L;
        RoleSaveDTO dto = new RoleSaveDTO();
        dto.setName("Updated Role");
        RoleDTO updatedDto = new RoleDTO();
        updatedDto.setId(id);
        updatedDto.setName("Updated Role");

        when(roleService.updateRole(dto)).thenReturn(updatedDto);

        RestResponse<RoleDTO> response = roleController.update(id, dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());
        assertEquals(id, dto.getId());

        verify(roleService).updateRole(dto);
    }

    @Test
    void updateState() {
        Long id = 1L;
        Integer state = 1;
        RoleDTO updatedDto = new RoleDTO();
        updatedDto.setId(id);
        updatedDto.setState(state);

        when(roleService.updateState(id, state)).thenReturn(updatedDto);

        RestResponse<RoleDTO> response = roleController.updateState(id, state);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());

        verify(roleService).updateState(id, state);
    }

    @Test
    void deleteById() {
        Long id = 1L;
        doNothing().when(roleService).deleteRoleBeforeValidation(id);

        RestResponse<Void> response = roleController.deleteById(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(roleService).deleteRoleBeforeValidation(id);
    }

    @Test
    void grant() {
        Long id = 1L;
        RoleGrantDTO grantDTO = new RoleGrantDTO();
        doNothing().when(roleService).grant(id, grantDTO);

        RestResponse<Void> response = roleController.grant(id, grantDTO);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(roleService).grant(id, grantDTO);
    }

    @Test
    void getRoleOptions() {
        RoleQuery query = new RoleQuery();
        List<RoleOptionDTO> options = Collections.singletonList(new RoleOptionDTO());

        when(roleService.getRoleOptions(query)).thenReturn(options);

        RestResponse<List<RoleOptionDTO>> response = roleController.getRoleOptions(query);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(options, response.getData());

        verify(roleService).getRoleOptions(query);
    }
}
