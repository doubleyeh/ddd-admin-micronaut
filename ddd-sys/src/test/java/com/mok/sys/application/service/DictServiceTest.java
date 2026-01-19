package com.mok.sys.application.service;

import com.mok.common.application.exception.BizException;
import com.mok.common.application.exception.NotFoundException;
import com.mok.sys.application.dto.dict.DictDataDTO;
import com.mok.sys.application.dto.dict.DictDataSaveDTO;
import com.mok.sys.application.dto.dict.DictTypeDTO;
import com.mok.sys.application.dto.dict.DictTypeQuery;
import com.mok.sys.application.dto.dict.DictTypeSaveDTO;
import com.mok.sys.application.mapper.DictDataMapper;
import com.mok.sys.application.mapper.DictTypeMapper;
import com.mok.sys.domain.model.DictData;
import com.mok.sys.domain.model.DictType;
import com.mok.sys.domain.repository.DictDataRepository;
import com.mok.sys.domain.repository.DictTypeRepository;
import com.mok.common.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DictServiceTest {

    private DictTypeRepository dictTypeRepository;
    private DictDataRepository dictDataRepository;
    private DictTypeMapper dictTypeMapper;
    private DictDataMapper dictDataMapper;
    private RedisCommands<String, String> redisCommands;
    private DictService dictService;

    @BeforeEach
    void setUp() {
        dictTypeRepository = mock(DictTypeRepository.class);
        dictDataRepository = mock(DictDataRepository.class);
        dictTypeMapper = mock(DictTypeMapper.class);
        dictDataMapper = mock(DictDataMapper.class);
        redisCommands = mock(RedisCommands.class);
        dictService = new DictService(dictTypeRepository, dictDataRepository, dictTypeMapper, dictDataMapper, redisCommands);
    }

    @Test
    void createType_Success() {
        DictTypeSaveDTO dto = new DictTypeSaveDTO();
        dto.setName("Test Type");
        dto.setCode("test_type");
        dto.setSort(1);
        dto.setRemark("A test type");

        when(dictTypeRepository.existsByCode("test_type")).thenReturn(false);

        dictService.createType(dto);

        ArgumentCaptor<DictType> captor = ArgumentCaptor.forClass(DictType.class);
        verify(dictTypeRepository).save(captor.capture());
        DictType savedEntity = captor.getValue();

        assertEquals("Test Type", savedEntity.getName());
        assertEquals("test_type", savedEntity.getCode());
        assertEquals(1, savedEntity.getSort());
        assertEquals("A test type", savedEntity.getRemark());
    }

    @Test
    void createType_CodeExists_ShouldThrowBizException() {
        DictTypeSaveDTO dto = new DictTypeSaveDTO();
        dto.setCode("existing_code");

        when(dictTypeRepository.existsByCode("existing_code")).thenReturn(true);

        Exception exception = assertThrows(BizException.class, () -> dictService.createType(dto));
        assertEquals("字典类型编码已存在", exception.getMessage());
    }

    @Test
    void updateType_Success() {
        DictTypeSaveDTO dto = new DictTypeSaveDTO();
        dto.setId(1L);
        dto.setName("Updated Name");
        dto.setCode("test_type");
        dto.setSort(2);
        dto.setRemark("Updated remark");

        DictType existingType = DictType.create("Old Name", "test_type", 1, "Old remark");
        

        when(dictTypeRepository.findById(1L)).thenReturn(Optional.of(existingType));

        dictService.updateType(dto);

        ArgumentCaptor<DictType> captor = ArgumentCaptor.forClass(DictType.class);
        verify(dictTypeRepository).save(captor.capture());
        DictType savedEntity = captor.getValue();

        assertEquals("Updated Name", savedEntity.getName());
        assertEquals(2, savedEntity.getSort());
        assertEquals("Updated remark", savedEntity.getRemark());
    }

@Test
    void updateType_SystemDictChangeCode_ShouldThrowBizException() {
        DictTypeSaveDTO dto = new DictTypeSaveDTO();
        dto.setId(1L);
        dto.setCode("new_code");

        DictType systemType = DictType.create("System Dict", "system_code", 1, "", true);
        

        when(dictTypeRepository.findById(1L)).thenReturn(Optional.of(systemType));

        Exception exception = assertThrows(BizException.class, () -> dictService.updateType(dto));
        assertEquals("系统内置字典禁止修改编码", exception.getMessage());
    }

    @Test
    void updateType_SystemDictSameCode_ShouldAllowUpdate() {
        DictTypeSaveDTO dto = new DictTypeSaveDTO();
        dto.setId(1L);
        dto.setCode("system_code");
        dto.setName("Updated Name");
        dto.setSort(2);
        dto.setRemark("Updated remark");

        DictType systemType = DictType.create("System Dict", "system_code", 1, "", true);
        

        when(dictTypeRepository.findById(1L)).thenReturn(Optional.of(systemType));
        when(dictTypeRepository.save(any())).thenReturn(systemType);
        when(dictTypeMapper.toDto(any())).thenReturn(new DictTypeDTO());

        DictTypeDTO result = dictService.updateType(dto);

        assertNotNull(result);
        verify(dictTypeRepository).save(systemType);
    }

    @Test
    void deleteType_Success() {
        Long typeId = 1L;
        DictType existingType = DictType.create("Test Type", "test_type", 1, "");
        

        when(dictTypeRepository.findById(typeId)).thenReturn(Optional.of(existingType));

        dictService.deleteType(typeId);

        verify(dictDataRepository).deleteByTypeCode("test_type");
        verify(redisCommands).del(Const.CacheKey.DICT_DATA + "test_type");
        verify(dictTypeRepository).delete(existingType);
    }

    @Test
    void deleteType_SystemDict_ShouldThrowBizException() {
        Long typeId = 1L;
        DictType systemType = DictType.create("System Dict", "system_code", 1, "", true);
        

        when(dictTypeRepository.findById(typeId)).thenReturn(Optional.of(systemType));

        Exception exception = assertThrows(BizException.class, () -> dictService.deleteType(typeId));
        assertEquals("系统内置字典禁止删除", exception.getMessage());
    }

    @Test
    void createData_Success() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setTypeCode("test_type");
        dto.setLabel("Label");
        dto.setValue("Value");

        DictType parentType = DictType.create("Test Type", "test_type", 1, "");
        
        when(dictTypeRepository.findByCode("test_type")).thenReturn(Optional.of(parentType));

        dictService.createData(dto);

        ArgumentCaptor<DictData> captor = ArgumentCaptor.forClass(DictData.class);
        verify(dictDataRepository).save(captor.capture());
        DictData savedEntity = captor.getValue();

        assertEquals("test_type", savedEntity.getTypeCode());
        assertEquals("Label", savedEntity.getLabel());
        assertEquals("Value", savedEntity.getValue());

        verify(redisCommands).del(Const.CacheKey.DICT_DATA + "test_type");
    }

    @Test
    void createData_ForSystemDict_ShouldThrowBizException() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setTypeCode("system_type");

        DictType parentType = DictType.create("System Type", "system_type", 1, "", true);
        when(dictTypeRepository.findByCode("system_type")).thenReturn(Optional.of(parentType));

        Exception exception = assertThrows(BizException.class, () -> dictService.createData(dto));
        assertEquals("系统内置字典禁止修改数据", exception.getMessage());
    }
    
    @Test
    void deleteData_Success() {
        Long dataId = 1L;
        DictData existingData = DictData.create("test_type", "Label", "Value", 1, null, null, false, null);
        
        DictType parentType = DictType.create("Test Type", "test_type", 1, "");
        
        
        when(dictDataRepository.findById(dataId)).thenReturn(Optional.of(existingData));
        when(dictTypeRepository.findByCode("test_type")).thenReturn(Optional.of(parentType));

        dictService.deleteData(dataId);

        verify(dictDataRepository).delete(existingData);
        verify(redisCommands).del(Const.CacheKey.DICT_DATA + "test_type");
    }
    
@Test
    void deleteData_NotFound_ShouldThrowNotFoundException() {
        when(dictDataRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dictService.deleteData(99L));
    }

    @Test
    void findPage_Success() {
        DictTypeQuery query = new DictTypeQuery();
        Pageable pageable = Pageable.from(0, 10);
        
        DictType dictType = DictType.create("Test Type", "test_type", 1, "Test remark");
        Page<DictType> entityPage = Page.of(Arrays.asList(dictType), Pageable.from(0, 10), 1L);
        
        when(dictTypeRepository.findAll(any(), any())).thenReturn(entityPage);
        when(dictTypeMapper.toDto(dictType)).thenReturn(new DictTypeDTO());
        
        Page<DictTypeDTO> result = dictService.findPage(query, pageable);
        
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(dictTypeRepository).findAll(any(), any());
    }

    @Test
    void getDataByType_Success() {
        String typeCode = "test_type";
        DictData dictData = DictData.create(typeCode, "Label", "Value", 1, null, null, false, null);
        
        when(dictDataRepository.findByTypeCodeOrderBySortAsc(typeCode))
                .thenReturn(Arrays.asList(dictData));
        when(dictDataMapper.toDto(dictData)).thenReturn(new DictDataDTO());
        
        List<DictDataDTO> result = dictService.getDataByType(typeCode);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(dictDataRepository).findByTypeCodeOrderBySortAsc(typeCode);
    }

    @Test
    void updateData_Success() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setId(1L);
        dto.setTypeCode("test_type");
        dto.setLabel("Updated Label");
        dto.setValue("Updated Value");
        dto.setSort(2);
        
        DictData existingData = DictData.create("test_type", "Old Label", "Old Value", 1, null, null, false, null);
        DictType parentType = DictType.create("Test Type", "test_type", 1, "");
        
        when(dictDataRepository.findById(1L)).thenReturn(Optional.of(existingData));
        when(dictTypeRepository.findByCode("test_type")).thenReturn(Optional.of(parentType));
        when(dictDataRepository.save(any())).thenReturn(existingData);
        when(dictDataMapper.toDto(any())).thenReturn(new DictDataDTO());
        
        DictDataDTO result = dictService.updateData(dto);
        
        assertNotNull(result);
        verify(dictDataRepository).save(any());
        verify(redisCommands).del(Const.CacheKey.DICT_DATA + "test_type");
    }

    @Test
    void updateData_NotFound_ShouldThrowNotFoundException() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setId(99L);
        
        when(dictDataRepository.findById(99L)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> dictService.updateData(dto));
    }

    @Test
    void updateData_SystemDict_ShouldThrowBizException() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setId(1L);
        dto.setTypeCode("system_type");
        
        DictData existingData = DictData.create("system_type", "Label", "Value", 1, null, null, false, null);
        DictType systemType = DictType.create("System Type", "system_type", 1, "", true);
        
        when(dictDataRepository.findById(1L)).thenReturn(Optional.of(existingData));
        when(dictTypeRepository.findByCode("system_type")).thenReturn(Optional.of(systemType));
        
        Exception exception = assertThrows(BizException.class, () -> dictService.updateData(dto));
        assertEquals("系统内置字典禁止修改数据", exception.getMessage());
    }

    @Test
    void updateData_TypeCodeChanged_ShouldClearBothCaches() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setId(1L);
        dto.setTypeCode("new_type");
        dto.setLabel("Updated Label");
        dto.setValue("Updated Value");
        
        DictData existingData = DictData.create("old_type", "Old Label", "Old Value", 1, null, null, false, null);
        DictType newType = DictType.create("New Type", "new_type", 1, "");
        
        // Create updated entity with new type code
        DictData updatedData = DictData.create("new_type", "Updated Label", "Updated Value", 1, null, null, false, null);
        
        when(dictDataRepository.findById(1L)).thenReturn(Optional.of(existingData));
        when(dictTypeRepository.findByCode("new_type")).thenReturn(Optional.of(newType));
        when(dictDataRepository.save(any())).thenReturn(updatedData);
        when(dictDataMapper.toDto(any())).thenReturn(new DictDataDTO());
        
        dictService.updateData(dto);
        
        verify(redisCommands).del(Const.CacheKey.DICT_DATA + "old_type");
        verify(redisCommands).del(Const.CacheKey.DICT_DATA + "new_type");
    }

    @Test
    void deleteType_NotFound_ShouldThrowNotFoundException() {
        when(dictTypeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dictService.deleteType(99L));
    }

    @Test
    void updateType_NotFound_ShouldThrowNotFoundException() {
        DictTypeSaveDTO dto = new DictTypeSaveDTO();
        dto.setId(99L);
        
        when(dictTypeRepository.findById(99L)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> dictService.updateType(dto));
    }

    @Test
    void createData_TypeNotFound_ShouldProceed() {
        DictDataSaveDTO dto = new DictDataSaveDTO();
        dto.setTypeCode("nonexistent_type");
        dto.setLabel("Label");
        dto.setValue("Value");
        
        when(dictTypeRepository.findByCode("nonexistent_type")).thenReturn(Optional.empty());
        when(dictDataRepository.save(any())).thenReturn(DictData.create("nonexistent_type", "Label", "Value", 1, null, null, false, null));
        when(dictDataMapper.toDto(any())).thenReturn(new DictDataDTO());
        
        DictDataDTO result = dictService.createData(dto);
        
        assertNotNull(result);
        verify(dictDataRepository).save(any());
        verify(redisCommands).del(Const.CacheKey.DICT_DATA + "nonexistent_type");
    }
}
