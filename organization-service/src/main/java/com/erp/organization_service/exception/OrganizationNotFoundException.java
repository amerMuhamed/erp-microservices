package com.erp.organization_service.exception;

public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(Long id) {
        super("Organization not found with id: " + id);
    }
}
