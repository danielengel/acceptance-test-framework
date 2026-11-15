package dev.acceptance.clients;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public final class ServiceOneClient {
    private final HttpClient http;
    private final Duration requestTimeout;
    private final URI endpoint;

    public ServiceOneClient(HttpClient http, String baseUrl, Duration requestTimeout) {
        this.http = http;
        this.endpoint = URI.create(baseUrl + "/entities");
        this.requestTimeout = requestTimeout;
    }

    public HttpResponse<String> create(String id, String json) throws Exception {
        return http.send(HttpRequest.newBuilder(endpoint).timeout(requestTimeout)
                .header("Content-Type", "application/json").header("X-Correlation-ID", id)
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
