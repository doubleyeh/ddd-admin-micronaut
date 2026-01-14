package com.mok.web.sys;


import com.mok.application.sys.dto.tenant.*;
import com.mok.application.sys.service.TenantService;
import com.mok.infrastructure.log.BusinessType;
import com.mok.infrastructure.log.OperLogRecord;
import com.mok.web.common.RestResponse;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import java.util.List;


@Controller("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @Get
    @Secured("hasAuthority('tenant:list')")
    public RestResponse<Page<@NonNull TenantDTO>> findPage(TenantQuery query, Pageable pageable) {
        Page<@NonNull TenantDTO> page = tenantService.findPage(query.toPredicate(), pageable);
        return RestResponse.success(page);
    }

    @Get("/{id}")
    @Secured("hasAuthority('tenant:list')")
    public RestResponse<TenantDTO> getById(@PathVariable Long id) {
        TenantDTO tenantDTO = tenantService.getById(id);
        return RestResponse.success(tenantDTO);
    }

    @Post
    @Secured("hasAuthority('tenant:create')")
    @OperLogRecord(title = "租户管理", businessType = BusinessType.INSERT)
    public RestResponse<TenantCreateResultDTO> create(@Body @Valid TenantSaveDTO tenantSaveDTO) {
        TenantCreateResultDTO result = tenantService.createTenant(tenantSaveDTO);
        return RestResponse.success(result);
    }

    @Put("/{id}")
    @Secured("hasAuthority('tenant:update')")
    @OperLogRecord(title = "租户管理", businessType = BusinessType.UPDATE)
    public RestResponse<TenantDTO> update(@PathVariable Long id, @Body @Valid TenantSaveDTO tenantSaveDTO) {
        tenantSaveDTO.setId(id);
        TenantDTO updatedTenant = tenantService.updateTenant(id, tenantSaveDTO);
        return RestResponse.success(updatedTenant);
    }

    @Put("/{id}/state")
    @Secured("hasAuthority('tenant:update')")
    @OperLogRecord(title = "租户管理", businessType = BusinessType.UPDATE)
    public RestResponse<TenantDTO> updateState(@PathVariable Long id,@Valid @NotNull @QueryValue Integer state) {
        TenantDTO updatedTenant = tenantService.updateTenantState(id, state);
        return RestResponse.success(updatedTenant);
    }

    @Delete("/{id}")
    @Secured("hasAuthority('tenant:delete')")
    @OperLogRecord(title = "租户管理", businessType = BusinessType.DELETE)
    public RestResponse<Void> deleteById(@PathVariable Long id) {
        tenantService.deleteByVerify(id);
        return RestResponse.success();
    }

    @Get("/options")
    public RestResponse<List<TenantOptionDTO>> getOptions(@QueryValue(defaultValue = "") String name) {
        return RestResponse.success(tenantService.findOptions(name));
    }
}
