package dev.acceptance.one;

import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.kinesis.KinesisClient;

@SpringBootApplication
public class ServiceOneApplication {
    public static void main(String[] args) { SpringApplication.run(ServiceOneApplication.class, args); }

    @Bean DynamoDbClient dynamo(@Value("${demo.aws-endpoint}") URI endpoint) {
        return DynamoDbClient.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofSeconds(10)).build()).build();
    }
    @Bean KinesisClient kinesis(@Value("${demo.aws-endpoint}") URI endpoint) {
        return KinesisClient.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofSeconds(10)).build()).build();
    }
}

@RestController
class EntityController {
    private final DynamoDbClient dynamo;
    private final KinesisClient kinesis;
    private final ObjectMapper json;
    private final String table;
    private final String stream;

    EntityController(DynamoDbClient dynamo, KinesisClient kinesis, ObjectMapper json,
                     @Value("${demo.table}") String table, @Value("${demo.stream}") String stream) {
        this.dynamo = dynamo; this.kinesis = kinesis; this.json = json; this.table = table; this.stream = stream;
    }

    record EntityRequest(String id, String name) {}

    @PostMapping("/entities")
    ResponseEntity<Map<String, String>> create(@RequestBody EntityRequest request) throws Exception {
        if (request.id() == null || request.id().isBlank() || request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id and name are required");
        }
        dynamo.putItem(b -> b.tableName(table).item(Map.of(
                "id", AttributeValue.fromS(request.id()), "name", AttributeValue.fromS(request.name()),
                "status", AttributeValue.fromS("CREATED"))));
        var bytes = SdkBytes.fromUtf8String(json.writeValueAsString(request));
        kinesis.putRecord(b -> b.streamName(stream).partitionKey(request.id()).data(bytes));
        return ResponseEntity.accepted().body(Map.of("id", request.id()));
    }

    @GetMapping("/ready") Map<String, String> ready() { return Map.of("status", "UP"); }
}
