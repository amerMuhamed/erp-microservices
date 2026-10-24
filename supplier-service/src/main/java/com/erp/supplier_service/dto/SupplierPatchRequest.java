package com.erp.supplier_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SupplierPatchRequest(

        @Positive(message = "Organization id must be positive")
        Long organizationId,

        @Size(max = 150, message = "Supplier name must not exceed 150 characters")
        @Pattern(regexp = ".*\\S.*", message = "Supplier name must not be blank")
        String name,

        @Size(max = 50, message = "Supplier code must not exceed 50 characters")
        @Pattern(regexp = ".*\\S.*", message = "Supplier code must not be blank")
        String code,

        @Email(message = "Email must be a valid address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        Boolean active
) {
}
