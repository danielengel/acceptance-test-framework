package dev.acceptance.clients;

import dev.acceptance.infrastructure.AcceptanceConfig;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import javax.net.ssl.TrustManagerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;

/** Scenario-scoped AWS clients using the emulator's credentials and public CA certificate. */
public record AwsClients(DynamoDbClient dynamo, KinesisClient kinesis) implements AutoCloseable {
    public static AwsClients create(AcceptanceConfig config) throws Exception {
        // Match Compose's CBOR_ENABLED=false: the emulator expects Kinesis JSON requests.
        // Set before constructing clients, because the SDK captures this setting at construction.
        System.setProperty("aws.cborEnabled", "false");
        var endpoint = config.emulatorEndpoint();
        if (!"https".equals(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null
                || endpoint.getRawUserInfo() != null
                || !(endpoint.getPath().isEmpty() || endpoint.getPath().equals("/"))) {
            throw new IllegalArgumentException("AWS endpoint must be an HTTPS origin without path or query");
        }
        var profile = ProfileFile.builder().content(config.emulatorCredentials())
                .type(ProfileFile.Type.CREDENTIALS).build();
        StaticCredentialsProvider credentials;
        try (var provider = ProfileCredentialsProvider.builder()
                .profileFile(profile).profileName("default").build()) {
            credentials = StaticCredentialsProvider.create(provider.resolveCredentials());
        }
        var store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        try (var input = Files.newInputStream(config.emulatorCaCertificate())) {
            store.setCertificateEntry("aws-gateway",
                    CertificateFactory.getInstance("X.509").generateCertificate(input));
        }
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        var transport = UrlConnectionHttpClient.builder()
                .tlsTrustManagersProvider(trust::getTrustManagers)
                .connectionTimeout(config.connectTimeout())
                .socketTimeout(config.requestTimeout());
        var overrides = ClientOverrideConfiguration.builder()
                .apiCallTimeout(config.requestTimeout())
                .apiCallAttemptTimeout(config.requestTimeout())
                .retryStrategy(b -> b.maxAttempts(1)).build();
        var dynamo = DynamoDbClient.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .credentialsProvider(credentials).httpClientBuilder(transport)
                .overrideConfiguration(overrides).build();
        try {
            var kinesis = KinesisClient.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                    .credentialsProvider(credentials).httpClientBuilder(transport)
                    .overrideConfiguration(overrides).build();
            return new AwsClients(dynamo, kinesis);
        } catch (RuntimeException e) {
            dynamo.close();
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            kinesis.close();
        } finally {
            dynamo.close();
        }
    }
}
