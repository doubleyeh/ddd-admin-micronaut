package com.mok.sys.application.mapper;

import com.mok.sys.application.dto.tenant.TenantCreateResultDTO;
import com.mok.sys.application.dto.tenant.TenantDTO;
import com.mok.sys.application.dto.tenant.TenantOptionDTO;
import com.mok.sys.domain.model.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "jsr330", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    TenantDTO toDto(Tenant entity);

    void updateEntityFromDto(Tenant entity, @MappingTarget TenantCreateResultDTO dto);

    List<TenantOptionDTO> dtoToOptionsDto(List<TenantDTO> dtoList);
}
