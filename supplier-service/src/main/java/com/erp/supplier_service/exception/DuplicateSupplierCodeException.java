package com.erp.supplier_service.exception;

public class DuplicateSupplierCodeException extends RuntimeException {

    public DuplicateSupplierCodeException(Long organizationId, String code) {
        super("Supplier code already exists for organization " + organizationId + ": " + code);
    }
}
