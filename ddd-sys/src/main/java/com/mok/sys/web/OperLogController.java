package com.mok.sys.web;

import com.mok.sys.application.dto.log.OperLogDTO;
import com.mok.sys.application.dto.log.OperLogQuery;
import com.mok.sys.application.service.OperLogService;
import com.mok.common.web.RestResponse;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller("/api/oper-logs")
public class OperLogController {

    private final OperLogService operLogService;

    @Get
    @Secured("hasAuthority('log:oper:list')")
    public RestResponse<Page<@NonNull OperLogDTO>> findPage(OperLogQuery query, Pageable pageable) {
        Page<@NonNull OperLogDTO> page = operLogService.findPage(query, pageable);
        return RestResponse.success(page);
    }
}
