package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.dict.DictDataDTO;
import com.mok.sys.domain.model.DictData;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JSR330, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictDataMapper {
    DictDataDTO toDto(DictData entity);
}
