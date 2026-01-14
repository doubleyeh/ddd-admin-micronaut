package com.mok.web.sys;

import com.mok.application.sys.dto.tenantPackage.*;
import com.mok.application.sys.service.TenantPackageService;
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


@Controller("/api/tenant-packages")
@RequiredArgsConstructor
public class TenantPackageController {

    private final TenantPackageService packageService;

    @Get
    @Secured("hasAuthority('tenantPackage:list')")
    public RestResponse<Page<@NonNull TenantPackageDTO>> findPage(TenantPackageQuery query, Pageable pageable) {
        Page<@NonNull TenantPackageDTO> page = packageService.findPage(query.toPredicate(), pageable);
        return RestResponse.success(page);
    }

    @Get("/{id}")
    @Secured("hasAuthority('tenantPackage:list')")
    public RestResponse<TenantPackageDTO> get(@PathVariable Long id) {
        return RestResponse.success(packageService.getById(id));
    }

    @Post
    @Secured("hasAuthority('tenantPackage:create')")
    @OperLogRecord(title = "套餐管理", businessType = BusinessType.INSERT)
    public RestResponse<Void> create(@Body TenantPackageSaveDTO dto) {
        packageService.createPackage(dto);
        return RestResponse.success();
    }

    @Put("/{id}")
    @Secured("hasAuthority('tenantPackage:update')")
    @OperLogRecord(title = "套餐管理", businessType = BusinessType.UPDATE)
    public RestResponse<Void> update(@PathVariable Long id, @Body TenantPackageSaveDTO dto) {
        packageService.updatePackage(id, dto);
        return RestResponse.success();
    }

    @Put("/{id}/grant")
    @Secured("hasAuthority('tenantPackage:update')")
    @OperLogRecord(title = "套餐管理", businessType = BusinessType.GRANT)
    public RestResponse<Void> grant(@PathVariable Long id, @Body TenantPackageGrantDTO dto) {
        packageService.grant(id, dto);
        return RestResponse.success();
    }

    @Put("/{id}/state")
    @Secured("hasAuthority('tenantPackage:update')")
    @OperLogRecord(title = "套餐管理", businessType = BusinessType.UPDATE)
    public RestResponse<TenantPackageDTO> updateState(@PathVariable Long id, @Valid @NotNull @QueryValue Integer state) {
        TenantPackageDTO updatedTenant = packageService.updateTenantState(id, state);
        return RestResponse.success(updatedTenant);
    }

    @Delete("/{id}")
    @Secured("hasAuthority('tenantPackage:delete')")
    @OperLogRecord(title = "套餐管理", businessType = BusinessType.DELETE)
    public RestResponse<Void> deleteById(@PathVariable Long id) {
        packageService.deleteByVerify(id);
        return RestResponse.success();
    }

    @Get("/options")
    public RestResponse<List<TenantPackageOptionDTO>> getOptions(@QueryValue(defaultValue = "") String name) {
        return RestResponse.success(packageService.findOptions(name));
    }

}
