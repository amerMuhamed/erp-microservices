package com.erp.organization_service.dto;

import com.erp.organization_service.entity.Organization;

import java.time.Instant;

public record OrganizationResponse(
        Long id,
        String name,
        String code,
        String email,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getEmail(),
                organization.isActive(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }
}
