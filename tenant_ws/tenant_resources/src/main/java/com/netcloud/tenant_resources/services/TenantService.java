package com.netcloud.tenant_resources.services;

import com.netcloud.commons.JwtUtil;
import com.netcloud.dtos.tenant.LoginResponse;
import com.netcloud.dtos.tenant.Tenant;
import com.netcloud.tenant_resources.exceptions.InvalidCredentialsException;
import com.netcloud.tenant_resources.exceptions.TenantNotFound;
import com.netcloud.tenant_resources.mappers.TenantModelDtoMapper;
import com.netcloud.tenant_resources.repository.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantModelDtoMapper tenantModelDtoMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository,
                         TenantModelDtoMapper tenantModelDtoMapper,
                         JwtUtil jwtUtil,
                         PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.tenantModelDtoMapper = tenantModelDtoMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public Tenant getTenantDetailsById(String id) {
        Optional<com.netcloud.models.tenant.Tenant> tenant = tenantRepository.findById(id);
        if (tenant.isPresent()) {
            return tenantModelDtoMapper.toDto(tenant.get());
        } else {
            throw new TenantNotFound();
        }
    }

    public Tenant addTenantDetails(com.netcloud.dtos.tenant.Tenant tenant) {
        if (tenant.getTenantId() == null || tenant.getTenantId().isEmpty()) {
            tenant.setTenantId(UUID.randomUUID().toString());
        }
        if (tenant.getPassword() != null && !tenant.getPassword().startsWith("$2")) {
            tenant.setPassword(passwordEncoder.encode(tenant.getPassword()));
        }
        return tenantModelDtoMapper.toDto(tenantRepository.save(
                tenantModelDtoMapper.toModel(tenant)));
    }

    public LoginResponse login(String email, String password) {
        Tenant model = tenantModelDtoMapper.toDto(tenantRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new));
        if (!passwordEncoder.matches(password, model.getPassword())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtUtil.generateToken(model.getTenantId(), model.getTenantName());
        return new LoginResponse(token, model.getTenantId(), model.getTenantName(), model.getEmail());
    }

    public LoginResponse registerTenant(Tenant tenant) {
        if (tenantRepository.findByEmail(tenant.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        tenant.setTenantId(UUID.randomUUID().toString());
        tenant.setPassword(passwordEncoder.encode(tenant.getPassword()));
        com.netcloud.models.tenant.Tenant saved = tenantRepository.save(
                tenantModelDtoMapper.toModel(tenant));
        String token = jwtUtil.generateToken(saved.getTenantId(), saved.getTenantName());
        return new LoginResponse(token, saved.getTenantId(), saved.getTenantName(), saved.getEmail());
    }
}
