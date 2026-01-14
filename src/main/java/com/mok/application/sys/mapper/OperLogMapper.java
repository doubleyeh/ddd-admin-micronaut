package com.mok.application.sys.mapper;

import com.mok.application.sys.dto.log.OperLogDTO;
import com.mok.domain.sys.model.OperLog;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JSR330)
public interface OperLogMapper {
    OperLogDTO toDto(OperLog log);
}
