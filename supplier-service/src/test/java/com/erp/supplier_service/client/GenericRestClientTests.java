package com.erp.supplier_service.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GenericRestClientTests {

    private static final String BASE_URL = "http://inventory-service:8083";

    private MockRestServiceServer server;
    private GenericRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GenericRestClient(builder.build());
    }

    @Test
    void getReturnsTypedBody() {
        server.expect(requestTo(BASE_URL + "/api/items/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("pong", MediaType.TEXT_PLAIN));

        String body = client.get(uri("/api/items/{id}", 7L), String.class);

        assertThat(body).isEqualTo("pong");
        server.verify();
    }

    @Test
    void getReturnsStatusWithoutReadingBody() {
        server.expect(requestTo(BASE_URL + "/api/items/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        var response = client.get(uri("/api/items/{id}", 7L));

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        server.verify();
    }

    @Test
    void postSendsBodyAndReturnsTypedBody() {
        server.expect(requestTo(BASE_URL + "/api/items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"name":"Widget"}
                        """))
                .andRespond(withSuccess("created", MediaType.TEXT_PLAIN));

        String body = client.post(uri("/api/items"), new ItemPayload("Widget"), String.class);

        assertThat(body).isEqualTo("created");
        server.verify();
    }

    @Test
    void putSendsBodyAndReturnsTypedBody() {
        server.expect(requestTo(BASE_URL + "/api/items/7"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {"name":"Widget"}
                        """))
                .andRespond(withSuccess("updated", MediaType.TEXT_PLAIN));

        String body = client.put(uri("/api/items/{id}", 7L), new ItemPayload("Widget"), String.class);

        assertThat(body).isEqualTo("updated");
        server.verify();
    }

    @Test
    void deleteExecutesRequest() {
        server.expect(requestTo(BASE_URL + "/api/items/7"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatCode(() -> client.delete(uri("/api/items/{id}", 7L))).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void httpErrorsArePropagatedUntranslated() {
        server.expect(requestTo(BASE_URL + "/api/items/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.get(uri("/api/items/{id}", 7L)))
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value())
                        .isEqualTo(404));
        server.verify();
    }

    private static URI uri(String path, Object... uriVariables) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .path(path)
                .buildAndExpand(uriVariables)
                .toUri();
    }

    private record ItemPayload(String name) {
    }
}
