package dev.acceptance.infrastructure;

import dev.acceptance.clients.AwsClients;
import dev.acceptance.clients.ServiceOneClient;
import dev.acceptance.clients.ServiceTwoClient;
import dev.acceptance.clients.ThirdPartyMock;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static dev.acceptance.support.AsyncAssertions.eventually;
import static org.assertj.core.api.Assertions.assertThat;

public final class AcceptanceEnvironment implements AutoCloseable {
    public final Path artifacts = Path.of("target", "acceptance-artifacts", UUID.randomUUID().toString()).toAbsolutePath();
    private final AcceptanceConfig config;
    public final ObjectMapper json = JsonMapper.builder().build();
    private final HttpClient http;
    public final ThirdPartyMock thirdParty;
    private final ServiceOneClient serviceOne;
    private final ServiceTwoClient serviceTwo;

    public AcceptanceEnvironment() {
        this(AcceptanceConfig.fromSystemProperties());
    }

    public AcceptanceEnvironment(AcceptanceConfig config) {
        this.config = config;
        http = HttpClient.newBuilder().connectTimeout(config.connectTimeout()).build();
        thirdParty = new ThirdPartyMock(config.wiremockBaseUrl());
        serviceOne = new ServiceOneClient(http, config.serviceOneBaseUrl(), config.requestTimeout());
        serviceTwo = new ServiceTwoClient(http, config.serviceTwoBaseUrl(), config.requestTimeout());
    }

    public AwsClients aws;

    public void start() throws Exception {
        Files.createDirectories(artifacts);
        aws = AwsClients.create(config);
        waitForStack();
    }

    private void waitForStack() {
        for (var url : new String[]{config.serviceOneBaseUrl() + "/ready",
                config.serviceTwoBaseUrl() + "/ready",
                thirdParty.baseUrl() + "/__admin/mappings"}) {
            eventually(url + " ready").untilAsserted(() -> {
                try {
                    var response = http.send(HttpRequest.newBuilder(URI.create(url))
                            .timeout(config.readinessTimeout()).build(), HttpResponse.BodyHandlers.discarding());
                    assertThat(response.statusCode()).isEqualTo(200);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    public ServiceOneClient serviceOne() {
        return serviceOne;
    }

    public ServiceTwoClient serviceTwo() {
        return serviceTwo;
    }

    public void diagnostics(String entityId) {
        // Best effort: diagnostics must never replace the original assertion failure.
        try {
            json.writerWithDefaultPrettyPrinter().writeValue(
                    artifacts.resolve("wiremock-requests.json").toFile(), thirdParty.requests());
        } catch (Exception ignored) {
        }
        try {
            var entity = aws.dynamo().getItem(b -> b.tableName("entities").consistentRead(true)
                    .key(Map.of("id", software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS(entityId))));
            Files.writeString(artifacts.resolve("entity.txt"), entity.toString());
        } catch (Exception ignored) {
        }
        System.err.println("Acceptance diagnostics: " + artifacts);
    }

    @Override
    public void close() {
        try {
            if (aws != null) aws.close();
        } finally {
            http.close();
        }
    }

}
