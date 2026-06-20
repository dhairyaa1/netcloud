package com.netcloud.dtos.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tenantId;
    private String tenantName;
    private String email;
}
