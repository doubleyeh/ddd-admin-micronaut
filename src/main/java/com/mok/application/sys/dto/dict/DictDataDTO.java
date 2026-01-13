package com.mok.application.sys.dto.dict;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Serdeable
public class DictDataDTO {
    private Long id;
    private String typeCode;
    private String label;
    private String value;
    private Integer sort;
    private String cssClass;
    private String listClass;
    private Boolean isDefault;
    private String remark;
    private LocalDateTime createTime;
}
