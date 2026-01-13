package com.mok.application.sys.mapper;

import com.mok.application.sys.dto.dict.DictDataDTO;
import com.mok.domain.sys.model.DictData;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JSR330, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictDataMapper {
    DictDataDTO toDto(DictData entity);
}
