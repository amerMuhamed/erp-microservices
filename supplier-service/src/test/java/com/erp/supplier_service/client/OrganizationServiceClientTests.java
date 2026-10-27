package com.erp.supplier_service.client;

import com.erp.supplier_service.exception.OrganizationReferenceNotFoundException;
import com.erp.supplier_service.exception.OrganizationServiceResponseException;
import com.erp.supplier_service.exception.OrganizationServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class OrganizationServiceClientTests {

    private static final String BASE_URL = "http://organization-service:8081";

    private MockRestServiceServer server;
    private OrganizationServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrganizationServiceClient(new GenericRestClient(builder.build()), BASE_URL);
    }

    @Test
    void existingOrganizationIsAccepted() {
        server.expect(requestTo(BASE_URL + "/api/organizations/42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        assertThatCode(() -> client.validateOrganizationExists(42L)).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void missingOrganizationThrowsReferenceNotFoundException() {
        server.expect(requestTo(BASE_URL + "/api/organizations/42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationReferenceNotFoundException.class)
                .hasMessageContaining("42");

        server.verify();
    }

    @Test
    void serverErrorThrowsServiceUnavailableException() {
        server.expect(requestTo(BASE_URL + "/api/organizations/42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceUnavailableException.class);

        server.verify();
    }

    @Test
    void connectionFailureThrowsServiceUnavailableException() {
        server.expect(requestTo(BASE_URL + "/api/organizations/42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceUnavailableException.class);

        server.verify();
    }

    @Test
    void unexpectedClientErrorIsNotReportedAsMissingOrganization() {
        server.expect(requestTo(BASE_URL + "/api/organizations/42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceResponseException.class)
                .isNotInstanceOf(OrganizationReferenceNotFoundException.class);

        server.verify();
    }
}
