package com.mok.sys.web;

import com.mok.sys.application.dto.dict.*;
import com.mok.sys.application.service.DictService;
import com.mok.common.web.RestResponse;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Controller("/api/dict")
public class DictController {

    private final DictService dictService;

    @Get("/type")
    @Secured("hasAuthority('dict:list')")
    public RestResponse<Page<DictTypeDTO>> findTypePage(DictTypeQuery query, Pageable pageable) {
        return RestResponse.success(dictService.findPage(query, pageable));
    }

    @Post("/type")
    @Secured("hasAuthority('dict:create')")
    public RestResponse<DictTypeDTO> createType(@Body @Valid DictTypeSaveDTO dto) {
        return RestResponse.success(dictService.createType(dto));
    }

    @Put("/type")
    @Secured("hasAuthority('dict:update')")
    public RestResponse<DictTypeDTO> updateType(@Body @Valid DictTypeSaveDTO dto) {
        return RestResponse.success(dictService.updateType(dto));
    }

    @Delete("/type/{id}")
    @Secured("hasAuthority('dict:delete')")
    public RestResponse<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return RestResponse.success();
    }

    @Get("/data/{typeCode}")
    public RestResponse<List<DictDataDTO>> getDataByType(@PathVariable String typeCode) {
        return RestResponse.success(dictService.getDataByType(typeCode));
    }

    @Post("/data")
    @Secured("hasAuthority('dict:create')")
    public RestResponse<DictDataDTO> createData(@Body @Valid DictDataSaveDTO dto) {
        return RestResponse.success(dictService.createData(dto));
    }

    @Put("/data")
    @Secured("hasAuthority('dict:update')")
    public RestResponse<DictDataDTO> updateData(@Body @Valid DictDataSaveDTO dto) {
        return RestResponse.success(dictService.updateData(dto));
    }

    @Delete("/data/{id}")
    @Secured("hasAuthority('dict:delete')")
    public RestResponse<Void> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return RestResponse.success();
    }
}
