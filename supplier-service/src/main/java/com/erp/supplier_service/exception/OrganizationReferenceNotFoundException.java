package com.erp.supplier_service.exception;

public class OrganizationReferenceNotFoundException extends RuntimeException {

    public OrganizationReferenceNotFoundException(Long organizationId) {
        super("Organization not found with id: " + organizationId);
    }
}
