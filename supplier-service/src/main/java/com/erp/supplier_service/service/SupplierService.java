package com.erp.supplier_service.service;

import com.erp.supplier_service.dto.SupplierPatchRequest;
import com.erp.supplier_service.dto.SupplierRequest;
import com.erp.supplier_service.dto.SupplierResponse;

import java.util.List;

public interface SupplierService {

    SupplierResponse create(SupplierRequest request);

    List<SupplierResponse> findAll();

    SupplierResponse findById(Long id);

    List<SupplierResponse> findByOrganizationId(Long organizationId);

    SupplierResponse update(Long id, SupplierRequest request);

    SupplierResponse patch(Long id, SupplierPatchRequest request);

    void delete(Long id);
}
