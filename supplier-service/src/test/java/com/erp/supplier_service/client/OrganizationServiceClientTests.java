package com.erp.supplier_service.client;

import com.erp.supplier_service.exception.OrganizationReferenceNotFoundException;
import com.erp.supplier_service.exception.OrganizationServiceResponseException;
import com.erp.supplier_service.exception.OrganizationServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class OrganizationServiceClientTests {

    private static final String BASE_URL = "http://organization-service:8081";
    private static final String ORGANIZATION_URL = BASE_URL + "/api/organizations/42";

    private MockRestServiceServer server;
    private CircuitBreaker circuitBreaker;
    private OrganizationServiceClient client;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordExceptions(OrganizationServiceUnavailableException.class)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker(OrganizationServiceClient.CIRCUIT_BREAKER_NAME);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrganizationServiceClient(new GenericRestClient(builder.build()), registry, BASE_URL);
    }

    @Test
    void existingOrganizationIsAccepted() {
        server.expect(requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        assertThatCode(() -> client.validateOrganizationExists(42L)).doesNotThrowAnyException();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        server.verify();
    }

    @Test
    void missingOrganizationThrowsReferenceNotFoundExceptionWithoutCountingAsFailure() {
        server.expect(ExpectedCount.times(5), requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                    .isInstanceOf(OrganizationReferenceNotFoundException.class)
                    .hasMessageContaining("42");
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        server.verify();
    }

    @Test
    void serverErrorCountsAsFailure() {
        server.expect(requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceUnavailableException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        server.verify();
    }

    @Test
    void connectionFailureCountsAsFailure() {
        server.expect(requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceUnavailableException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        server.verify();
    }

    @Test
    void timeoutCountsAsFailure() {
        server.expect(requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceUnavailableException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        server.verify();
    }

    @Test
    void unexpectedClientErrorIsNotReportedAsMissingOrganizationAndDoesNotCountAsFailure() {
        server.expect(requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceResponseException.class)
                .isNotInstanceOf(OrganizationReferenceNotFoundException.class);

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        server.verify();
    }
}
