package com.erp.supplier_service;

import com.erp.supplier_service.client.OrganizationServiceClient;
import com.erp.supplier_service.entity.Supplier;
import com.erp.supplier_service.exception.OrganizationReferenceNotFoundException;
import com.erp.supplier_service.exception.OrganizationServiceUnavailableException;
import com.erp.supplier_service.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SupplierApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepository;

    @MockitoBean
    private OrganizationServiceClient organizationServiceClient;

    @Test
    void createSupplierReturnsCreatedSupplier() throws Exception {
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": 9001,
                                  "name": "Dell",
                                  "code": "DELL-001",
                                  "email": "dell@example.com",
                                  "phone": "+20123456789",
                                  "address": "Cairo"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/suppliers/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.organizationId").value(9001))
                .andExpect(jsonPath("$.name").value("Dell"))
                .andExpect(jsonPath("$.code").value("DELL-001"))
                .andExpect(jsonPath("$.email").value("dell@example.com"))
                .andExpect(jsonPath("$.phone").value("+20123456789"))
                .andExpect(jsonPath("$.address").value("Cairo"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getSupplierByIdReturnsSupplier() throws Exception {
        long id = createSupplier(9002, "SUP-001", "Dell");

        mockMvc.perform(get("/api/suppliers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.organizationId").value(9002))
                .andExpect(jsonPath("$.code").value("SUP-001"));
    }

    @Test
    void getSuppliersByOrganizationReturnsOnlyThatOrganizationsSuppliers() throws Exception {
        createSupplier(9003, "SUP-A", "Alpha");
        createSupplier(9003, "SUP-B", "Beta");
        createSupplier(9004, "SUP-A", "Alpha Other Organization");

        mockMvc.perform(get("/api/organizations/{organizationId}/suppliers", 9003))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].organizationId").value(9003))
                .andExpect(jsonPath("$[1].organizationId").value(9003));
    }

    @Test
    void putPerformsFullUpdate() throws Exception {
        long id = createSupplierWithContact(9005, "SUP-PUT", "Dell", "old@example.com", "0100", "Cairo");

        mockMvc.perform(put("/api/suppliers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": 9005,
                                  "name": "Dell Updated",
                                  "code": "SUP-PUT-2",
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dell Updated"))
                .andExpect(jsonPath("$.code").value("SUP-PUT-2"))
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.phone").value(nullValue()))
                .andExpect(jsonPath("$.address").value(nullValue()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void patchPreservesFieldsNotProvided() throws Exception {
        long id = createSupplierWithContact(9006, "SUP-PATCH", "Original", "keep@example.com", "0111", "Alexandria");

        mockMvc.perform(patch("/api/suppliers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Patched Name"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Patched Name"))
                .andExpect(jsonPath("$.code").value("SUP-PATCH"))
                .andExpect(jsonPath("$.email").value("keep@example.com"))
                .andExpect(jsonPath("$.phone").value("0111"))
                .andExpect(jsonPath("$.address").value("Alexandria"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void patchActiveFalseIsAppliedAndPreserved() throws Exception {
        long id = createSupplier(9007, "SUP-ACTIVE", "Dell");

        mockMvc.perform(patch("/api/suppliers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/suppliers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Renamed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void createFailsWithNotFoundWhenOrganizationDoesNotExist() throws Exception {
        willThrow(new OrganizationReferenceNotFoundException(9100L))
                .given(organizationServiceClient).validateOrganizationExists(9100L);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": 9100,
                                  "name": "Missing Org Supplier",
                                  "code": "ORG-MISSING"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Organization not found")));

        assertThat(supplierRepository.existsByOrganizationIdAndCode(9100L, "ORG-MISSING")).isFalse();
    }

    @Test
    void createFailsWithServiceUnavailableWhenOrganizationServiceIsDown() throws Exception {
        willThrow(new OrganizationServiceUnavailableException("organization-service is unavailable"))
                .given(organizationServiceClient).validateOrganizationExists(9101L);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": 9101,
                                  "name": "Unavailable Org Supplier",
                                  "code": "ORG-UNAVAILABLE"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message", containsString("unavailable")));

        assertThat(supplierRepository.existsByOrganizationIdAndCode(9101L, "ORG-UNAVAILABLE")).isFalse();
    }

    @Test
    void duplicateCodeWithinSameOrganizationIsRejected() throws Exception {
        createSupplier(9008, "DUP-CODE", "First");

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": 9008,
                                  "name": "Second",
                                  "code": "DUP-CODE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void sameCodeIsAllowedUnderDifferentOrganizations() throws Exception {
        createSupplier(9009, "SHARED-CODE", "Dell at EME");
        createSupplier(9010, "SHARED-CODE", "Dell at ABC");

        mockMvc.perform(get("/api/organizations/{organizationId}/suppliers", 9009))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("SHARED-CODE"));

        mockMvc.perform(get("/api/organizations/{organizationId}/suppliers", 9010))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("SHARED-CODE"));
    }

    @Test
    void putRejectsDuplicateCodeWithinSameOrganization() throws Exception {
        createSupplier(9011, "EXISTING", "First");
        long secondId = createSupplier(9011, "SECOND", "Second");

        mockMvc.perform(put("/api/suppliers/{id}", secondId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": 9011,
                                  "name": "Second",
                                  "code": "EXISTING"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getMissingSupplierReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/suppliers/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Supplier not found")));
    }

    @Test
    void deleteSupplierRemovesIt() throws Exception {
        long id = createSupplier(9012, "SUP-DELETE", "To Delete");

        mockMvc.perform(delete("/api/suppliers/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/suppliers/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": 0,
                                  "name": "",
                                  "code": "",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.organizationId").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    private long createSupplier(long organizationId, String code, String name) throws Exception {
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": %d,
                                  "name": "%s",
                                  "code": "%s"
                                }
                                """.formatted(organizationId, name, code)))
                .andExpect(status().isCreated());

        return findSupplierId(organizationId, code);
    }

    private long createSupplierWithContact(
            long organizationId,
            String code,
            String name,
            String email,
            String phone,
            String address
    ) throws Exception {
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": %d,
                                  "name": "%s",
                                  "code": "%s",
                                  "email": "%s",
                                  "phone": "%s",
                                  "address": "%s"
                                }
                                """.formatted(organizationId, name, code, email, phone, address)))
                .andExpect(status().isCreated());

        return findSupplierId(organizationId, code);
    }

    private long findSupplierId(long organizationId, String code) {
        return supplierRepository.findAllByOrganizationIdOrderByIdAsc(organizationId).stream()
                .filter(supplier -> supplier.getCode().equals(code))
                .map(Supplier::getId)
                .findFirst()
                .orElseThrow();
    }
}
