package com.mok.sys.web;

import com.mok.sys.application.dto.dict.*;
import com.mok.sys.application.service.DictService;
import com.mok.common.web.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DictControllerTest {

    private DictService dictService;
    private DictController dictController;

    @BeforeEach
    void setUp() {
        dictService = mock(DictService.class);
        dictController = new DictController(dictService);
    }

    @Test
    void findTypePage() {
        DictTypeQuery query = new DictTypeQuery();
        Pageable pageable = Pageable.from(0, 10);
        Page<DictTypeDTO> page = Page.of(Collections.emptyList(), pageable, 0L);

        when(dictService.findPage(query, pageable)).thenReturn(page);

        RestResponse<Page<DictTypeDTO>> response = dictController.findTypePage(query, pageable);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(page, response.getData());

        verify(dictService).findPage(query, pageable);
    }

    @Test
    void createType() {
        DictTypeSaveDTO dto = new DictTypeSaveDTO();
        dto.setName("Test Type");
        dto.setCode("test_type");

        DictTypeDTO createdDto = new DictTypeDTO();
        createdDto.setId(1L);
        createdDto.setName("Test Type");
        createdDto.setCode("test_type");

        when(dictService.createType(dto)).thenReturn(createdDto);

        RestResponse<DictTypeDTO> response = dictController.createType(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(createdDto, response.getData());

        verify(dictService).createType(dto);
    }

    @Test
    void updateType() {
        DictTypeSaveDTO dto = new DictTypeSaveDTO();
        dto.setId(1L);
        dto.setName("Updated Type");

        DictTypeDTO updatedDto = new DictTypeDTO();
        updatedDto.setId(1L);
        updatedDto.setName("Updated Type");

        when(dictService.updateType(dto)).thenReturn(updatedDto);

        RestResponse<DictTypeDTO> response = dictController.updateType(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());

        verify(dictService).updateType(dto);
    }

    @Test
    void deleteType() {
        Long id = 1L;
        doNothing().when(dictService).deleteType(id);

        RestResponse<Void> response = dictController.deleteType(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(dictService).deleteType(id);
    }

    @Test
    void getDataByType() {
        String typeCode = "test_type";
        List<DictDataDTO> dataList = Collections.singletonList(new DictDataDTO());

        when(dictService.getDataByType(typeCode)).thenReturn(dataList);

        RestResponse<List<DictDataDTO>> response = dictController.getDataByType(typeCode);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(dataList, response.getData());

        verify(dictService).getDataByType(typeCode);
    }

    @Test
    void createData() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setTypeCode("test_type");
        dto.setLabel("Label");
        dto.setValue("Value");

        DictDataDTO createdDto = new DictDataDTO();
        createdDto.setId(1L);
        createdDto.setTypeCode("test_type");

        when(dictService.createData(dto)).thenReturn(createdDto);

        RestResponse<DictDataDTO> response = dictController.createData(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(createdDto, response.getData());

        verify(dictService).createData(dto);
    }

    @Test
    void updateData() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setId(1L);
        dto.setLabel("Updated Label");

        DictDataDTO updatedDto = new DictDataDTO();
        updatedDto.setId(1L);
        updatedDto.setLabel("Updated Label");

        when(dictService.updateData(dto)).thenReturn(updatedDto);

        RestResponse<DictDataDTO> response = dictController.updateData(dto);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());
        assertEquals(updatedDto, response.getData());

        verify(dictService).updateData(dto);
    }

    @Test
    void deleteData() {
        Long id = 1L;
        doNothing().when(dictService).deleteData(id);

        RestResponse<Void> response = dictController.deleteData(id);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.isState());

        verify(dictService).deleteData(id);
    }
}
