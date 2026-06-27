package com.netcloud.tenant_resources.exceptions;

public class TenantNotFound extends RuntimeException {
    public TenantNotFound() {
        super("Invalid Tenant Id" );
    }
}
