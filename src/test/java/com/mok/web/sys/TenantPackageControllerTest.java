package com.mok.web.sys;

import com.mok.application.sys.dto.tenantPackage.*;
import com.mok.application.sys.service.TenantPackageService;
import com.mok.web.common.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantPackageControllerTest {

    private TenantPackageService packageService;
    private TenantPackageController packageController;

    @BeforeEach
    void setUp() {
        packageService = mock(TenantPackageService.class);
        packageController = new TenantPackageController(packageService);
    }

    @Test
    void findPage() {
        TenantPackageQuery query = new TenantPackageQuery();
        Pageable pageable = Pageable.from(0, 10);
        Page<TenantPackageDTO> page = Page.of(Collections.emptyList(), pageable, 0L);

        when(packageService.findPage(query, pageable)).thenReturn(page);

        RestResponse<Page<TenantPackageDTO>> response = packageController.findPage(query, pageable);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(page, response.getData());

        verify(packageService).findPage(query, pageable);
    }

    @Test
    void get() {
        Long id = 1L;
        TenantPackageDTO dto = new TenantPackageDTO();
        dto.setId(id);

        when(packageService.getById(id)).thenReturn(dto);

        RestResponse<TenantPackageDTO> response = packageController.get(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(dto, response.getData());

        verify(packageService).getById(id);
    }

    @Test
    void create() {
        TenantPackageSaveDTO dto = new TenantPackageSaveDTO();
        doNothing().when(packageService).createPackage(dto);

        RestResponse<Void> response = packageController.create(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(packageService).createPackage(dto);
    }

    @Test
    void update() {
        Long id = 1L;
        TenantPackageSaveDTO dto = new TenantPackageSaveDTO();
        doNothing().when(packageService).updatePackage(id, dto);

        RestResponse<Void> response = packageController.update(id, dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(packageService).updatePackage(id, dto);
    }

    @Test
    void grant() {
        Long id = 1L;
        TenantPackageGrantDTO dto = new TenantPackageGrantDTO();
        doNothing().when(packageService).grant(id, dto);

        RestResponse<Void> response = packageController.grant(id, dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(packageService).grant(id, dto);
    }

    @Test
    void updateState() {
        Long id = 1L;
        Integer state = 1;
        TenantPackageDTO dto = new TenantPackageDTO();
        dto.setId(id);
        dto.setState(state);

        when(packageService.updateTenantState(id, state)).thenReturn(dto);

        RestResponse<TenantPackageDTO> response = packageController.updateState(id, state);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(dto, response.getData());

        verify(packageService).updateTenantState(id, state);
    }

    @Test
    void deleteById() {
        Long id = 1L;
        doNothing().when(packageService).deleteByVerify(id);

        RestResponse<Void> response = packageController.deleteById(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(packageService).deleteByVerify(id);
    }

    @Test
    void getOptions() {
        String name = "test";
        List<TenantPackageOptionDTO> options = Collections.singletonList(new TenantPackageOptionDTO());
        when(packageService.findOptions(name)).thenReturn(options);

        RestResponse<List<TenantPackageOptionDTO>> response = packageController.getOptions(name);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(options, response.getData());

        verify(packageService).findOptions(name);
    }
}