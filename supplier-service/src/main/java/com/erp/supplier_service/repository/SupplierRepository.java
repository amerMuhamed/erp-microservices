package com.erp.supplier_service.repository;

import com.erp.supplier_service.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByOrganizationIdAndCode(Long organizationId, String code);

    boolean existsByOrganizationIdAndCodeAndIdNot(Long organizationId, String code, Long id);

    List<Supplier> findAllByOrganizationIdOrderByIdAsc(Long organizationId);

    List<Supplier> findAllByOrderByIdAsc();
}
