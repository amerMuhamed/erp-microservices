package com.erp.supplier_service.client;

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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class OrganizationCircuitBreakerTests {

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
    void opensAfterFailureThresholdIsReached() {
        expectFailures(5);

        openBreaker();

        server.verify();
    }

    @Test
    void rejectsCallsWhileOpenWithoutCallingOrganizationService() {
        expectFailures(5);
        server.expect(ExpectedCount.never(), requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        openBreaker();

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open");

        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1);
        server.verify();
    }

    @Test
    void transitionsToHalfOpenAfterWaitDuration() throws InterruptedException {
        expectFailures(5);
        server.expect(requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        openBreaker();
        Thread.sleep(150);

        client.validateOrganizationExists(42L);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        server.verify();
    }

    @Test
    void closesAfterPermittedSuccessfulCallsInHalfOpenState() throws InterruptedException {
        expectFailures(5);
        server.expect(ExpectedCount.times(2), requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        openBreaker();
        Thread.sleep(150);

        client.validateOrganizationExists(42L);
        client.validateOrganizationExists(42L);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        server.verify();
    }

    @Test
    void reopensWhenHalfOpenCallFails() throws InterruptedException {
        expectFailures(5);
        server.expect(ExpectedCount.times(2), requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        openBreaker();
        Thread.sleep(150);

        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceUnavailableException.class);
        assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                .isInstanceOf(OrganizationServiceUnavailableException.class);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        server.verify();
    }

    private void expectFailures(int count) {
        server.expect(ExpectedCount.times(count), requestTo(ORGANIZATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private void openBreaker() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> client.validateOrganizationExists(42L))
                    .isInstanceOf(OrganizationServiceUnavailableException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
