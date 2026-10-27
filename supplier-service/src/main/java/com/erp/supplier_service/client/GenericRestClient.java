package com.erp.supplier_service.client;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Component
public class GenericRestClient {

    private final RestClient restClient;

    public GenericRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public <T> T get(URI uri, Class<T> responseType) {
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);
    }

    public ResponseEntity<Void> get(URI uri) {
        return restClient.get()
                .uri(uri)
                .retrieve()
                .toBodilessEntity();
    }

    public <T, R> R post(URI uri, T requestBody, Class<R> responseType) {
        return restClient.post()
                .uri(uri)
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }

    public <T, R> R put(URI uri, T requestBody, Class<R> responseType) {
        return restClient.put()
                .uri(uri)
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }

    public void delete(URI uri) {
        restClient.delete()
                .uri(uri)
                .retrieve()
                .toBodilessEntity();
    }
}
