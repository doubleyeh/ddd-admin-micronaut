package com.mok.application.sys.service;

import com.mok.application.exception.BizException;
import com.mok.application.sys.dto.tenant.TenantCreateResultDTO;
import com.mok.application.sys.dto.tenant.TenantDTO;
import com.mok.application.sys.dto.tenant.TenantOptionDTO;
import com.mok.application.sys.dto.tenant.TenantSaveDTO;
import com.mok.application.sys.event.TenantCreatedEvent;
import com.mok.application.sys.mapper.TenantMapper;
import com.mok.domain.sys.model.Tenant;
import com.mok.domain.sys.repository.TenantRepository;
import com.mok.infrastructure.common.Const;
import com.mok.infrastructure.util.PasswordGenerator;
import com.mok.infrastructure.util.SysUtil;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final ApplicationEventPublisher<TenantCreatedEvent> eventPublisher;
    private final RedisCommands<String, String> redisCommands;

    @Transactional
    public TenantCreateResultDTO createTenant(@NonNull TenantSaveDTO dto) {
        Tenant tenant = Tenant.create(dto.getName(), dto.getContactPerson(), dto.getContactPhone(), dto.getPackageId(), tenantRepository);
        tenant = tenantRepository.save(tenant);

        String rawPassword = PasswordGenerator.generateRandomPassword();

        eventPublisher.publishEvent(new TenantCreatedEvent(tenant, rawPassword));

        TenantCreateResultDTO result = new TenantCreateResultDTO();
        result.setId(tenant.getId());
        result.setTenantId(tenant.getTenantId());
        result.setName(tenant.getName());
        result.setContactPerson(tenant.getContactPerson());
        result.setContactPhone(tenant.getContactPhone());
        result.setState(tenant.getState());
        result.setInitialAdminPassword(rawPassword);

        return result;
    }

    @Transactional
    public TenantDTO updateTenant(@NonNull Long id, @NonNull TenantSaveDTO dto) {
        Tenant existingTenant = tenantRepository.findById(id).orElseThrow(() -> new BizException("租户不存在"));

        if (!existingTenant.getTenantId().equals(dto.getTenantId())) {
            throw new BizException("租户编码不可修改");
        }

        existingTenant.updateInfo(dto.getName(), dto.getContactPerson(), dto.getContactPhone());
        existingTenant.changePackage(dto.getPackageId());

        Tenant savedTenant = tenantRepository.save(existingTenant);
        redisCommands.del(Const.CacheKey.TENANT + savedTenant.getTenantId());
        return tenantMapper.toDto(savedTenant);
    }

    @Transactional
    public TenantDTO updateTenantState(@NonNull Long id, @NonNull Integer state) {
        Tenant existingTenant = tenantRepository.findById(id).orElseThrow(() -> new BizException("租户不存在"));

        if (Objects.equals(state, Const.TenantState.NORMAL)) {
            existingTenant.enable();
        } else if (Objects.equals(state, Const.TenantState.DISABLED)) {
            existingTenant.disable();
        } else {
            throw new BizException("无效的状态值: " + state);
        }

        Tenant savedTenant = tenantRepository.save(existingTenant);
        redisCommands.del(Const.CacheKey.TENANT + savedTenant.getTenantId());
        return tenantMapper.toDto(savedTenant);
    }

    @Transactional
    public boolean deleteByVerify(@NonNull Long id) {
        TenantDTO old = tenantMapper.toDto(tenantRepository.findById(id).orElseThrow(() -> new BizException("租户不存在")));
        if (SysUtil.isSuperTenant(old.getTenantId())) {
            throw new BizException("该租户不可删除");
        }

        // TODO其他业务数据判断
        tenantRepository.deleteById(id);
        redisCommands.del(Const.CacheKey.TENANT + old.getTenantId());
        return true;
    }

    @Transactional(readOnly = true)
    public List<TenantOptionDTO> findOptions(String name) {
        List<Tenant> tenants;
        if (StringUtils.isNotEmpty(name)) {
            tenants = tenantRepository.findByNameContainsIgnoreCaseAndState(name, Const.TenantState.NORMAL);
        } else {
            tenants = tenantRepository.findByState(Const.TenantState.NORMAL);
        }
        return tenants.stream().map(tenantMapper::toDto).map(dto -> new TenantOptionDTO(dto.getId(), dto.getTenantId(), dto.getName())).collect(Collectors.toList());
    }
}
