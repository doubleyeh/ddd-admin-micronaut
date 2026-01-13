package com.mok.application.sys.mapper;

import com.mok.application.sys.dto.dict.DictTypeDTO;
import com.mok.domain.sys.model.DictType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JSR330, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictTypeMapper {
    DictTypeDTO toDto(DictType entity);
}
