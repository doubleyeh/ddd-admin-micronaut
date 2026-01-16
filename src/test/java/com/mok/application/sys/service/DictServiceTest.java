package com.mok.application.sys.service;

import com.mok.application.exception.BizException;
import com.mok.application.exception.NotFoundException;
import com.mok.application.sys.dto.dict.DictDataSaveDTO;
import com.mok.application.sys.dto.dict.DictTypeSaveDTO;
import com.mok.application.sys.mapper.DictDataMapper;
import com.mok.application.sys.mapper.DictTypeMapper;
import com.mok.domain.sys.model.DictData;
import com.mok.domain.sys.model.DictType;
import com.mok.domain.sys.repository.DictDataRepository;
import com.mok.domain.sys.repository.DictTypeRepository;
import com.mok.infrastructure.common.Const;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
}
