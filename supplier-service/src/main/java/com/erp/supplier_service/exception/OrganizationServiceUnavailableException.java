package com.erp.supplier_service.exception;

public class OrganizationServiceUnavailableException extends RuntimeException {

    public OrganizationServiceUnavailableException(String message) {
        super(message);
    }

    public OrganizationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
