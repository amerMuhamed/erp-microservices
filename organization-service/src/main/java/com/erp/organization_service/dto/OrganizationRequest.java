package com.erp.organization_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload used for both creating and updating an organization.
 * {@code active} is optional on creation (defaults to true) and, when omitted on update,
 * the current value is preserved.
 */
public record OrganizationRequest(

        @NotBlank(message = "Organization name is required")
        @Size(max = 150, message = "Organization name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Organization code is required")
        @Size(max = 50, message = "Organization code must not exceed 50 characters")
        String code,

        @Email(message = "Email must be a valid address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        Boolean active
) {
}
