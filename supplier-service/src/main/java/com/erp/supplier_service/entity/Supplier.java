package com.erp.supplier_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
        name = "suppliers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_suppliers_organization_code",
                columnNames = {"organization_id", "code"}
        ),
        indexes = @Index(
                name = "idx_suppliers_organization_id",
                columnList = "organization_id"
        )
)
public class Supplier extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Organization id is required")
    @Positive(message = "Organization id must be positive")
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @NotBlank(message = "Supplier name is required")
    @Size(max = 150, message = "Supplier name must not exceed 150 characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Supplier code is required")
    @Size(max = 50, message = "Supplier code must not exceed 50 characters")
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Email(message = "Email must be a valid address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Column(name = "email", length = 150)
    private String email;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    @Column(name = "phone", length = 30)
    private String phone;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Supplier() {
    }

    public Supplier(Long organizationId, String name, String code, String email, String phone, String address, boolean active) {
        this.organizationId = organizationId;
        this.name = name;
        this.code = code;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.active = active;
    }
}
