package com.erp.organization_service.dto;

import com.erp.organization_service.entity.Organization;

import java.util.Date;

public record OrganizationResponse(
        Long id,
        String name,
        String code,
        String email,
        boolean active,
        Date createdDate,
        Date modifiedDate
) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCode(),
                organization.getEmail(),
                organization.isActive(),
                organization.getCreatedDate(),
                organization.getModifiedDate()
        );
    }
}
