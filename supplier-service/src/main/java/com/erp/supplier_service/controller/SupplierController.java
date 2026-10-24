package com.erp.supplier_service.controller;

import com.erp.supplier_service.dto.SupplierPatchRequest;
import com.erp.supplier_service.dto.SupplierRequest;
import com.erp.supplier_service.dto.SupplierResponse;
import com.erp.supplier_service.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        SupplierResponse created = supplierService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierResponse>> findAll() {
        return ResponseEntity.ok(supplierService.findAll());
    }

    @GetMapping("/suppliers/{id}")
    public ResponseEntity<SupplierResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @GetMapping("/organizations/{organizationId}/suppliers")
    public ResponseEntity<List<SupplierResponse>> findByOrganizationId(@PathVariable Long organizationId) {
        return ResponseEntity.ok(supplierService.findByOrganizationId(organizationId));
    }

    @PutMapping("/suppliers/{id}")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request
    ) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @PatchMapping("/suppliers/{id}")
    public ResponseEntity<SupplierResponse> patch(
            @PathVariable Long id,
            @Valid @RequestBody SupplierPatchRequest request
    ) {
        return ResponseEntity.ok(supplierService.patch(id, request));
    }

    @DeleteMapping("/suppliers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
