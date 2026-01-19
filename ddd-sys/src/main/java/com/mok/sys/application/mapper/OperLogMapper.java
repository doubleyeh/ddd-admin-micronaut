package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.log.OperLogDTO;
import com.mok.sys.domain.model.OperLog;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JSR330)
public interface OperLogMapper {
    OperLogDTO toDto(OperLog log);
}
