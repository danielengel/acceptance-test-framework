package dev.acceptance.two;

import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;

@SpringBootApplication
@EnableScheduling
public class ServiceTwoApplication {
    public static void main(String[] args) { SpringApplication.run(ServiceTwoApplication.class, args); }

    @Bean KinesisClient kinesis(@Value("${demo.aws-endpoint}") URI endpoint) {
        return KinesisClient.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofSeconds(10)).build()).build();
    }
}

@Component
class EventConsumer {
    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);
    private final KinesisClient kinesis;
    private final ObjectMapper json;
    private final String stream;
    private final URI thirdParty;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private String iterator;
    private volatile boolean ready;

    EventConsumer(KinesisClient kinesis, ObjectMapper json, @Value("${demo.stream}") String stream,
                  @Value("${demo.third-party-url}") String thirdParty) {
        this.kinesis = kinesis; this.json = json; this.stream = stream;
        this.thirdParty = URI.create(thirdParty + "/notifications");
    }

    record EntityEvent(String id, String name) {}
    record Notification(String entityId, String name) {}

    @Scheduled(fixedDelay = 200)
    void poll() {
        try {
            if (iterator == null) {
                var shards = kinesis.listShards(b -> b.streamName(stream)).shards();
                if (shards.size() != 1) throw new IllegalStateException("Demo consumer requires exactly one shard");
                iterator = kinesis.getShardIterator(b -> b.streamName(stream).shardId(shards.getFirst().shardId())
                        .shardIteratorType(ShardIteratorType.TRIM_HORIZON)).shardIterator();
            }
            var batch = kinesis.getRecords(b -> b.shardIterator(iterator).limit(100));
            for (var record : batch.records()) {
                var event = json.readValue(record.data().asUtf8String(), EntityEvent.class);
                var body = json.writeValueAsString(new Notification(event.id(), event.name()));
                var response = http.send(HttpRequest.newBuilder(thirdParty).timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json").header("X-Correlation-ID", event.id())
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() / 100 != 2) throw new IllegalStateException("Third party returned " + response.statusCode());
            }
            // On failure retain this iterator and retry the batch. Duplicate deliveries are possible.
            iterator = batch.nextShardIterator();
            ready = iterator != null;
        } catch (Exception failure) {
            ready = false;
            log.warn("Consumer poll failed; batch will be retried", failure);
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    boolean ready() { return ready; }
}

@RestController
class ReadinessController {
    private final EventConsumer consumer;
    ReadinessController(EventConsumer consumer) { this.consumer = consumer; }
    @GetMapping("/ready") ResponseEntity<Map<String, String>> ready() {
        return ResponseEntity.status(consumer.ready() ? 200 : 503)
                .body(Map.of("status", consumer.ready() ? "UP" : "DOWN"));
    }
}
