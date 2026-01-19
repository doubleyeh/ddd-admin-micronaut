package com.mok.sys.domain.model;

import com.mok.common.domain.BaseEntity;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sys_dict_type")
@Introspected
public class DictType extends BaseEntity {

    /**
     * 字典名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 字典类型编码 (如: sys_user_sex)
     */
    @Column(unique = true, nullable = false)
    private String code;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否系统内置 (true: 禁止删除/修改代码)
     */
    @Column(name = "is_system")
    private Boolean isSystem;

    public static DictType create(@NonNull String name, @NonNull String code, Integer sort, String remark) {
        DictType dictType = new DictType();
        dictType.name = name;
        dictType.code = code;
        dictType.sort = sort;
        dictType.remark = remark;
        dictType.isSystem = false;
        return dictType;
    }

    public static DictType create(@NonNull String name, @NonNull String code, Integer sort, String remark, Boolean isSystem) {
        DictType dictType = new DictType();
        dictType.name = name;
        dictType.code = code;
        dictType.sort = sort;
        dictType.remark = remark;
        dictType.isSystem = isSystem;
        return dictType;
    }

    public void updateInfo(@NonNull String name, Integer sort, String remark) {
        this.name = name;
        this.sort = sort;
        this.remark = remark;
    }
}
