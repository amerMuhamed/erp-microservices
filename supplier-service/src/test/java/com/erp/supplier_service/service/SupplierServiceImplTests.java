package com.erp.supplier_service.service;

import com.erp.supplier_service.client.OrganizationServiceClient;
import com.erp.supplier_service.dto.SupplierRequest;
import com.erp.supplier_service.dto.SupplierResponse;
import com.erp.supplier_service.entity.Supplier;
import com.erp.supplier_service.exception.DuplicateSupplierCodeException;
import com.erp.supplier_service.exception.OrganizationReferenceNotFoundException;
import com.erp.supplier_service.exception.OrganizationServiceUnavailableException;
import com.erp.supplier_service.repository.SupplierRepository;
import com.erp.supplier_service.service.impl.SupplierServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTests {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private OrganizationServiceClient organizationServiceClient;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Test
    void createPersistsSupplierWhenOrganizationExists() {
        given(supplierRepository.existsByOrganizationIdAndCode(1L, "DELL-001")).willReturn(false);
        given(supplierRepository.save(any(Supplier.class))).willAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse response = supplierService.create(request(1L, "DELL-001"));

        assertThat(response.organizationId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Dell");
        assertThat(response.code()).isEqualTo("DELL-001");
        verify(organizationServiceClient).validateOrganizationExists(1L);
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createFailsAndSavesNothingWhenOrganizationDoesNotExist() {
        willThrow(new OrganizationReferenceNotFoundException(99L))
                .given(organizationServiceClient).validateOrganizationExists(99L);

        assertThatThrownBy(() -> supplierService.create(request(99L, "DELL-001")))
                .isInstanceOf(OrganizationReferenceNotFoundException.class);

        verifyNoInteractions(supplierRepository);
    }

    @Test
    void createFailsAndSavesNothingWhenOrganizationServiceIsUnavailable() {
        willThrow(new OrganizationServiceUnavailableException("organization-service is unavailable"))
                .given(organizationServiceClient).validateOrganizationExists(1L);

        assertThatThrownBy(() -> supplierService.create(request(1L, "DELL-001")))
                .isInstanceOf(OrganizationServiceUnavailableException.class);

        verifyNoInteractions(supplierRepository);
    }

    @Test
    void duplicateCodeWithinOrganizationIsStillRejected() {
        given(supplierRepository.existsByOrganizationIdAndCode(1L, "DUP-CODE")).willReturn(true);

        assertThatThrownBy(() -> supplierService.create(request(1L, "DUP-CODE")))
                .isInstanceOf(DuplicateSupplierCodeException.class);

        verify(organizationServiceClient).validateOrganizationExists(1L);
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    private static SupplierRequest request(Long organizationId, String code) {
        return new SupplierRequest(organizationId, "Dell", code, null, null, null, true);
    }
}
