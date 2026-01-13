package com.mok.application.sys.dto.dict;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Serdeable
public class DictTypeDTO {
    private Long id;
    private String name;
    private String code;
    private Integer sort;
    private String remark;
    private Boolean isSystem;
    private LocalDateTime createTime;
}
