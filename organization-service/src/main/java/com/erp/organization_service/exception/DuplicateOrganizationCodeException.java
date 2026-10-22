package com.erp.organization_service.exception;

public class DuplicateOrganizationCodeException extends RuntimeException {

    public DuplicateOrganizationCodeException(String code) {
        super("Organization code already exists: " + code);
    }
}
