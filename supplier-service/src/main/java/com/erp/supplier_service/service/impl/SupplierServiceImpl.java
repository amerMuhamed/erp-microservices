package com.erp.supplier_service.service.impl;

import com.erp.supplier_service.client.OrganizationServiceClient;
import com.erp.supplier_service.dto.SupplierPatchRequest;
import com.erp.supplier_service.dto.SupplierRequest;
import com.erp.supplier_service.dto.SupplierResponse;
import com.erp.supplier_service.entity.Supplier;
import com.erp.supplier_service.exception.DuplicateSupplierCodeException;
import com.erp.supplier_service.exception.InvalidRequestException;
import com.erp.supplier_service.exception.SupplierNotFoundException;
import com.erp.supplier_service.repository.SupplierRepository;
import com.erp.supplier_service.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final OrganizationServiceClient organizationServiceClient;

    @Override
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        organizationServiceClient.validateOrganizationExists(request.organizationId());

        String code = normalizeRequired(request.code());
        if (supplierRepository.existsByOrganizationIdAndCode(request.organizationId(), code)) {
            throw new DuplicateSupplierCodeException(request.organizationId(), code);
        }

        Supplier supplier = new Supplier(
                request.organizationId(),
                normalizeRequired(request.name()),
                code,
                normalizeOptional(request.email()),
                normalizeOptional(request.phone()),
                normalizeOptional(request.address()),
                request.active() == null || request.active()
        );

        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @Override
    public List<SupplierResponse> findAll() {
        return supplierRepository.findAllByOrderByIdAsc().stream()
                .map(SupplierResponse::from)
                .toList();
    }

    @Override
    public SupplierResponse findById(Long id) {
        return SupplierResponse.from(getSupplierOrThrow(id));
    }

    @Override
    public List<SupplierResponse> findByOrganizationId(Long organizationId) {
        if (organizationId == null || organizationId <= 0) {
            throw new InvalidRequestException("Organization id must be positive");
        }

        return supplierRepository.findAllByOrganizationIdOrderByIdAsc(organizationId).stream()
                .map(SupplierResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = getSupplierOrThrow(id);
        String code = normalizeRequired(request.code());

        if (supplierRepository.existsByOrganizationIdAndCodeAndIdNot(request.organizationId(), code, id)) {
            throw new DuplicateSupplierCodeException(request.organizationId(), code);
        }

        supplier.setOrganizationId(request.organizationId());
        supplier.setName(normalizeRequired(request.name()));
        supplier.setCode(code);
        supplier.setEmail(normalizeOptional(request.email()));
        supplier.setPhone(normalizeOptional(request.phone()));
        supplier.setAddress(normalizeOptional(request.address()));
        supplier.setActive(request.active() == null || request.active());

        return SupplierResponse.from(supplierRepository.saveAndFlush(supplier));
    }

    @Override
    @Transactional
    public SupplierResponse patch(Long id, SupplierPatchRequest request) {
        Supplier supplier = getSupplierOrThrow(id);

        Long targetOrganizationId = request.organizationId() != null
                ? request.organizationId()
                : supplier.getOrganizationId();
        String targetCode = request.code() != null
                ? request.code().trim()
                : supplier.getCode();

        boolean organizationChanged = !targetOrganizationId.equals(supplier.getOrganizationId());
        boolean codeChanged = !targetCode.equals(supplier.getCode());
        if ((organizationChanged || codeChanged)
                && supplierRepository.existsByOrganizationIdAndCodeAndIdNot(targetOrganizationId, targetCode, id)) {
            throw new DuplicateSupplierCodeException(targetOrganizationId, targetCode);
        }

        if (request.organizationId() != null) {
            supplier.setOrganizationId(request.organizationId());
        }
        if (request.name() != null) {
            supplier.setName(request.name().trim());
        }
        if (request.code() != null) {
            supplier.setCode(targetCode);
        }
        if (request.email() != null) {
            supplier.setEmail(normalizeOptional(request.email()));
        }
        if (request.phone() != null) {
            supplier.setPhone(normalizeOptional(request.phone()));
        }
        if (request.address() != null) {
            supplier.setAddress(normalizeOptional(request.address()));
        }
        if (request.active() != null) {
            supplier.setActive(request.active());
        }

        return SupplierResponse.from(supplierRepository.saveAndFlush(supplier));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        supplierRepository.delete(getSupplierOrThrow(id));
    }

    private Supplier getSupplierOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));
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
