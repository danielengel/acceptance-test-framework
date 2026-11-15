package dev.acceptance.infrastructure;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

/** Settings captured once for an acceptance scenario. */
public record AcceptanceConfig(
        String serviceOneBaseUrl,
        String serviceTwoBaseUrl,
        String wiremockBaseUrl,
        URI emulatorEndpoint,
        Path emulatorCredentials,
        Path emulatorCaCertificate,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration readinessTimeout) {

    public static AcceptanceConfig fromSystemProperties() {
        var runtime = Path.of(System.getProperty("emulator.runtime", "target/host-runtime"));
        return new AcceptanceConfig(
                System.getProperty("service.one.base.url", "http://localhost:18080/service-one"),
                System.getProperty("service.two.base.url", "http://localhost:18080/service-two"),
                System.getProperty("wiremock.base.url", "http://localhost:8080"),
                URI.create(System.getProperty("emulator.endpoint", "https://localhost:9443/")),
                Path.of(System.getProperty("emulator.credentials", runtime.resolve("emulator_credentials").toString())),
                Path.of(System.getProperty("emulator.ca.certificate", runtime.resolve("ca.pem").toString())),
                Duration.ofSeconds(3), Duration.ofSeconds(10), Duration.ofSeconds(2));
    }
}
