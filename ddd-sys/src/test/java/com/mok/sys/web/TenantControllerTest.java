package com.mok.sys.web;


import com.mok.sys.application.dto.tenant.*;
import com.mok.sys.application.service.TenantService;
import com.mok.common.web.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class TenantControllerTest {

    private TenantService tenantService;
    private TenantController tenantController;

    @BeforeEach
    void setUp() {
        tenantService = mock(TenantService.class);
        tenantController = new TenantController(tenantService);
    }

    @Test
    void findPage() {
        TenantQuery query = new TenantQuery();
        Pageable pageable = Pageable.from(0, 10);
        Page<TenantDTO> page = Page.of(Collections.emptyList(), pageable, 0L);

        when(tenantService.findPage(query, pageable)).thenReturn(page);

        RestResponse<Page<TenantDTO>> response = tenantController.findPage(query, pageable);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(page, response.getData());

        verify(tenantService).findPage(query, pageable);
    }

    @Test
    void getById() {
        Long id = 1L;
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setId(id);

        when(tenantService.getById(id)).thenReturn(tenantDTO);

        RestResponse<TenantDTO> response = tenantController.getById(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(tenantDTO, response.getData());

        verify(tenantService).getById(id);
    }

    @Test
    void create() {
        TenantSaveDTO tenantSaveDTO = new TenantSaveDTO();
        TenantCreateResultDTO result = new TenantCreateResultDTO();
        when(tenantService.createTenant(tenantSaveDTO)).thenReturn(result);

        RestResponse<TenantCreateResultDTO> response = tenantController.create(tenantSaveDTO);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(result, response.getData());

        verify(tenantService).createTenant(tenantSaveDTO);
    }

    @Test
    void update() {
        Long id = 1L;
        TenantSaveDTO tenantSaveDTO = new TenantSaveDTO();
        TenantDTO updatedTenant = new TenantDTO();
        when(tenantService.updateTenant(id, tenantSaveDTO)).thenReturn(updatedTenant);

        RestResponse<TenantDTO> response = tenantController.update(id, tenantSaveDTO);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedTenant, response.getData());
        assertEquals(id, tenantSaveDTO.getId());

        verify(tenantService).updateTenant(id, tenantSaveDTO);
    }

    @Test
    void updateState() {
        Long id = 1L;
        Integer state = 1;
        TenantDTO updatedTenant = new TenantDTO();
        when(tenantService.updateTenantState(id, state)).thenReturn(updatedTenant);

        RestResponse<TenantDTO> response = tenantController.updateState(id, state);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedTenant, response.getData());

        verify(tenantService).updateTenantState(id, state);
    }

    @Test
    void deleteById() {
        Long id = 1L;
        when(tenantService.deleteByVerify(id)).thenReturn(true);

        RestResponse<Void> response = tenantController.deleteById(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(tenantService).deleteByVerify(id);
    }

    @Test
    void getOptions() {
        String name = "test";
        List<TenantOptionDTO> options = Collections.singletonList(new TenantOptionDTO());
        when(tenantService.findOptions(name)).thenReturn(options);

        RestResponse<List<TenantOptionDTO>> response = tenantController.getOptions(name);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(options, response.getData());

        verify(tenantService).findOptions(name);
    }
}
