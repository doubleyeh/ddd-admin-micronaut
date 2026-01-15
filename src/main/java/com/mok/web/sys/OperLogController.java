package com.mok.web.sys;

import com.mok.application.sys.dto.log.OperLogDTO;
import com.mok.application.sys.dto.log.OperLogQuery;
import com.mok.application.sys.service.OperLogService;
import com.mok.web.common.RestResponse;
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
