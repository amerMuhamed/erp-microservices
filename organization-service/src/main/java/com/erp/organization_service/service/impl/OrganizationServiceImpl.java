package com.erp.organization_service.service.impl;

import com.erp.organization_service.dto.OrganizationRequest;
import com.erp.organization_service.dto.OrganizationResponse;
import com.erp.organization_service.entity.Organization;
import com.erp.organization_service.exception.DuplicateOrganizationCodeException;
import com.erp.organization_service.exception.OrganizationNotFoundException;
import com.erp.organization_service.repository.OrganizationRepository;
import com.erp.organization_service.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional
    public OrganizationResponse create(OrganizationRequest request) {
        String code = normalizeRequired(request.code());
        if (organizationRepository.existsByCode(code)) {
            throw new DuplicateOrganizationCodeException(code);
        }

        boolean active = request.active() == null || request.active();
        Organization organization = new Organization(
                normalizeRequired(request.name()),
                code,
                normalizeOptional(request.email()),
                active
        );

        return OrganizationResponse.from(organizationRepository.save(organization));
    }

    @Override
    public List<OrganizationResponse> findAll() {
        return organizationRepository.findAll().stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @Override
    public OrganizationResponse findById(Long id) {
        return OrganizationResponse.from(getOrganizationOrThrow(id));
    }

    @Override
    @Transactional
    public OrganizationResponse update(Long id, OrganizationRequest request) {
        Organization organization = getOrganizationOrThrow(id);
        String code = normalizeRequired(request.code());

        if (organizationRepository.existsByCodeAndIdNot(code, id)) {
            throw new DuplicateOrganizationCodeException(code);
        }

        organization.setName(normalizeRequired(request.name()));
        organization.setCode(code);
        organization.setEmail(normalizeOptional(request.email()));
        if (request.active() != null) {
            organization.setActive(request.active());
        }
        return OrganizationResponse.from(organizationRepository.saveAndFlush(organization));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Organization organization = getOrganizationOrThrow(id);
        organizationRepository.delete(organization);
    }

    private Organization getOrganizationOrThrow(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
