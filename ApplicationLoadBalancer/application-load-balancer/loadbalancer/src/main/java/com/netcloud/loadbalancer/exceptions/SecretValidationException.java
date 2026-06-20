package com.netcloud.loadbalancer.exceptions;

public class SecretValidationException extends RuntimeException {
    public SecretValidationException(String message) {
        super(message);
    }

    public SecretValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
