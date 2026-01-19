package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.dict.DictTypeDTO;
import com.mok.sys.domain.model.DictType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JSR330, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictTypeMapper {
    DictTypeDTO toDto(DictType entity);
}
