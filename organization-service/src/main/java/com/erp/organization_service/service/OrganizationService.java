package com.erp.organization_service.service;

import com.erp.organization_service.dto.OrganizationRequest;
import com.erp.organization_service.dto.OrganizationResponse;

import java.util.List;

public interface OrganizationService {

    OrganizationResponse create(OrganizationRequest request);

    List<OrganizationResponse> findAll();

    OrganizationResponse findById(Long id);

    OrganizationResponse update(Long id, OrganizationRequest request);

    void delete(Long id);
}
