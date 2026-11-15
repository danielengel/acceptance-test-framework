package dev.acceptance.clients;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class ServiceTwoClient {
    private final HttpClient http;
    private final URI endpoint;
    private final Duration requestTimeout;

    public ServiceTwoClient(HttpClient http, String baseUrl, Duration requestTimeout) {
        this.http = http;
        this.endpoint = URI.create(baseUrl + "/hello-world");
        this.requestTimeout = requestTimeout;
    }

    public HttpResponse<String> greeting() throws Exception {
        return http.send(HttpRequest.newBuilder(endpoint).timeout(requestTimeout).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
