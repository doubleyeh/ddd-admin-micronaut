package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.log.LoginLogDTO;
import com.mok.sys.domain.model.LoginLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JSR330)
public interface LoginLogMapper {
    @Mapping(target = "tenantName", ignore = true)
    LoginLogDTO toDto(LoginLog loginLog);
}
