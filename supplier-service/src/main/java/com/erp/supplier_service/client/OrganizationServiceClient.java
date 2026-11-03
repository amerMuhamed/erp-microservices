package com.erp.supplier_service.client;

import com.erp.supplier_service.exception.OrganizationReferenceNotFoundException;
import com.erp.supplier_service.exception.OrganizationServiceResponseException;
import com.erp.supplier_service.exception.OrganizationServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class OrganizationServiceClient {

    public static final String CIRCUIT_BREAKER_NAME = "organizationService";

    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceClient.class);

    private final GenericRestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final String baseUrl;

    public OrganizationServiceClient(
            GenericRestClient restClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.organization.base-url}") String baseUrl
    ) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        this.baseUrl = baseUrl;
    }

    public void validateOrganizationExists(Long organizationId) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/organizations/{id}")
                .buildAndExpand(organizationId)
                .toUri();

        try {
            circuitBreaker.executeRunnable(() -> callOrganizationService(organizationId, uri));
        } catch (CallNotPermittedException ex) {
            log.warn("Circuit breaker '{}' is open, rejecting validation of organization {}",
                    CIRCUIT_BREAKER_NAME, organizationId);
            throw new OrganizationServiceUnavailableException(
                    "organization-service is unavailable while validating organization " + organizationId
                            + " (circuit breaker is open)", ex);
        }
    }

    private void callOrganizationService(Long organizationId, URI uri) {
        try {
            ResponseEntity<Void> response = restClient.get(uri);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new OrganizationServiceResponseException(
                        organizationId, response.getStatusCode().value());
            }
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new OrganizationReferenceNotFoundException(organizationId);
            }
            if (status.is5xxServerError()) {
                log.warn("organization-service responded with {} for organization {}",
                        status.value(), organizationId);
                throw new OrganizationServiceUnavailableException(
                        "organization-service is unavailable while validating organization " + organizationId);
            }
            throw new OrganizationServiceResponseException(organizationId, status.value());
        } catch (ResourceAccessException ex) {
            log.warn("organization-service is unreachable while validating organization {}", organizationId, ex);
            throw new OrganizationServiceUnavailableException(
                    "organization-service is unavailable while validating organization " + organizationId, ex);
        }
    }
}
