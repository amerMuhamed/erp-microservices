package com.erp.supplier_service.exception;

public class OrganizationServiceResponseException extends RuntimeException {

    public OrganizationServiceResponseException(Long organizationId, int status) {
        super("organization-service returned unexpected status " + status
                + " while validating organization " + organizationId);
    }
}
