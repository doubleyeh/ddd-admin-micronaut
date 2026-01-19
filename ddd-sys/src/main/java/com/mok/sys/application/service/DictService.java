package com.mok.sys.application.service;

import com.mok.common.application.exception.BizException;
import com.mok.common.application.exception.NotFoundException;
import com.mok.sys.application.dto.dict.*;
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
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class DictService {

    private final DictTypeRepository dictTypeRepository;
    private final DictDataRepository dictDataRepository;
    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;
    private final RedisCommands<String, String> redisCommands;

    @Transactional(readOnly = true)
    public Page<DictTypeDTO> findPage(DictTypeQuery query, Pageable pageable) {
        Page<DictType> entityPage = dictTypeRepository.findAll(query.toPredicate(), pageable);
        return entityPage.map(dictTypeMapper::toDto);
    }

    @Transactional
    public DictTypeDTO createType(DictTypeSaveDTO dto) {
        if (dictTypeRepository.existsByCode(dto.getCode())) {
            throw new BizException("字典类型编码已存在");
        }
        DictType entity = DictType.create(dto.getName(), dto.getCode(), dto.getSort(), dto.getRemark());
        return dictTypeMapper.toDto(dictTypeRepository.save(entity));
    }

    @Transactional
    public DictTypeDTO updateType(DictTypeSaveDTO dto) {
        DictType entity = dictTypeRepository.findById(dto.getId())
                .orElseThrow(NotFoundException::new);

        if (Boolean.TRUE.equals(entity.getIsSystem())) {
            if (!entity.getCode().equals(dto.getCode())) {
                throw new BizException("系统内置字典禁止修改编码");
            }
        }

        entity.updateInfo(dto.getName(), dto.getSort(), dto.getRemark());
        return dictTypeMapper.toDto(dictTypeRepository.save(entity));
    }

    @Transactional
    public void deleteType(Long id) {
        DictType entity = dictTypeRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        if (Boolean.TRUE.equals(entity.getIsSystem())) {
            throw new BizException("系统内置字典禁止删除");
        }

        dictDataRepository.deleteByTypeCode(entity.getCode());
        redisCommands.del(Const.CacheKey.DICT_DATA + entity.getCode());

        dictTypeRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<DictDataDTO> getDataByType(String typeCode) {
        return dictDataRepository.findByTypeCodeOrderBySortAsc(typeCode).stream()
                .map(dictDataMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DictDataDTO createData(DictDataSaveDTO dto) {
        checkSystemDict(dto.getTypeCode());
        DictData entity = DictData.create(dto.getTypeCode(), dto.getLabel(), dto.getValue(), dto.getSort(), dto.getCssClass(), dto.getListClass(), dto.getIsDefault(), dto.getRemark());
        DictData saved = dictDataRepository.save(entity);
        redisCommands.del(Const.CacheKey.DICT_DATA + dto.getTypeCode());
        return dictDataMapper.toDto(saved);
    }

    @Transactional
    public DictDataDTO updateData(DictDataSaveDTO dto) {
        DictData entity = dictDataRepository.findById(dto.getId())
                .orElseThrow(NotFoundException::new);
        checkSystemDict(entity.getTypeCode());

        String oldTypeCode = entity.getTypeCode();
        entity.updateInfo(dto.getLabel(), dto.getValue(), dto.getSort(), dto.getCssClass(), dto.getListClass(), dto.getIsDefault(), dto.getRemark());
        DictData saved = dictDataRepository.save(entity);

        redisCommands.del(Const.CacheKey.DICT_DATA + oldTypeCode);
        if (!oldTypeCode.equals(saved.getTypeCode())) {
            redisCommands.del(Const.CacheKey.DICT_DATA + saved.getTypeCode());
        }

        return dictDataMapper.toDto(saved);
    }

    @Transactional
    public void deleteData(Long id) {
        DictData entity = dictDataRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        checkSystemDict(entity.getTypeCode());
        dictDataRepository.delete(entity);
        redisCommands.del(Const.CacheKey.DICT_DATA + entity.getTypeCode());
    }

    private void checkSystemDict(String typeCode) {
        dictTypeRepository.findByCode(typeCode).ifPresent(type -> {
            if (Boolean.TRUE.equals(type.getIsSystem())) {
                throw new BizException("系统内置字典禁止修改数据");
            }
        });
    }
}
