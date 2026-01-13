package com.mok.application.sys.mapper;

import com.mok.application.sys.dto.log.LoginLogDTO;
import com.mok.domain.sys.model.LoginLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JSR330)
public interface LoginLogMapper {
    @Mapping(target = "tenantName", ignore = true)
    LoginLogDTO toDto(LoginLog loginLog);
}
