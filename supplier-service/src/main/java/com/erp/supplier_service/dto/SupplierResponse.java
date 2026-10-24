package com.erp.supplier_service.dto;

import com.erp.supplier_service.entity.Supplier;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        Long organizationId,
        String name,
        String code,
        String email,
        String phone,
        String address,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getOrganizationId(),
                supplier.getName(),
                supplier.getCode(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
