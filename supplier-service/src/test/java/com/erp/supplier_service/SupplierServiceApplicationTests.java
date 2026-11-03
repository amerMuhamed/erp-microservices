package com.erp.supplier_service;

import com.erp.supplier_service.client.OrganizationServiceClient;
import com.erp.supplier_service.exception.OrganizationReferenceNotFoundException;
import com.erp.supplier_service.exception.OrganizationServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SupplierServiceApplicationTests {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void contextLoads() {
    }

    @Test
    void organizationServiceCircuitBreakerIsConfigured() {
        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker(OrganizationServiceClient.CIRCUIT_BREAKER_NAME);
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();

        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(10_000L);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
        assertThat(config.getRecordExceptionPredicate()
                .test(new OrganizationServiceUnavailableException("unavailable"))).isTrue();
        assertThat(config.getRecordExceptionPredicate()
                .test(new OrganizationReferenceNotFoundException(1L))).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
