package dev.acceptance.scenarios;

import dev.acceptance.infrastructure.AcceptanceEnvironment;
import dev.acceptance.support.TestScenario;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.net.http.HttpResponse;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import java.util.concurrent.atomic.AtomicReference;

import static dev.acceptance.support.AsyncAssertions.eventually;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class AcceptanceSteps {
    private TestScenario scenario;
    private AcceptanceEnvironment env;
    private HttpResponse<String> response;
    private AtomicReference<String> iterator;
    private final String path = "/notifications";
    private String expected;

    @Before
    public void startEnvironment() throws Exception {
        assumeTrue(Boolean.getBoolean("acceptance.enabled"), "Enable acceptance tests with -Pacceptance or -Dacceptance.enabled=true");
        scenario = TestScenario.unique();
        env = new AcceptanceEnvironment();
        env.start();
    }

    @After
    public void captureFailure(Scenario cucumberScenario) {
        try {
            if (cucumberScenario.isFailed() && env != null) {
                env.diagnostics(scenario.entityId());
                cucumberScenario.log("Acceptance diagnostics: " + env.artifacts);
            }
        } finally {
            if (env != null) env.close();
        }
    }

    @Given("the third party accepts notifications for a new entity")
    public void prepareNotification() throws Exception {
        expected = scenario.fixture("third-party-request.json");
        env.thirdParty.stubSuccess(path, expected);
    }

    @Given("the event stream is observed before entity creation")
    public void observeStream() throws Exception {
        var shards = env.aws.kinesis().listShards(b -> b.streamName("events")).shards();
        assertThat(shards).as("demo stream shard count").hasSize(1);
        iterator = new AtomicReference<>(env.aws.kinesis().getShardIterator(b -> b
                .streamName("events").shardId(shards.getFirst().shardId())
                .shardIteratorType(ShardIteratorType.LATEST)).shardIterator());

    }


    @When("I create the entity")
    public void createEntity() throws Exception {
        response = env.serviceOne().create(scenario.entityId(), scenario.fixture("create-entity.json"));
    }

    @Then("the response status is {int}")
    public void responseStatus(int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
    }

    @Then("the response contains the entity ID")
    public void responseEntityId() {
        assertThat(response.body()).contains(scenario.entityId());
    }

    @Then("the entity is stored with status CREATED")
    public void storedEntity() {
        eventually("entity persisted").untilAsserted(() -> {
            var entity = env.aws.dynamo().getItem(b -> b.tableName("entities").consistentRead(true)
                    .key(Map.of("id", AttributeValue.fromS(scenario.entityId())))).item();
            assertThat(entity).containsEntry("id", AttributeValue.fromS(scenario.entityId()))
                    .containsEntry("name", AttributeValue.fromS("Acceptance example"))
                    .containsEntry("status", AttributeValue.fromS("CREATED"));
        });
    }


    @Then("an event containing the entity is published")
    public void publishedEvent() {
        eventually("Kinesis event published").untilAsserted(() -> {
            var batch = env.aws.kinesis().getRecords(b -> b.shardIterator(iterator.get()).limit(100));
            iterator.set(batch.nextShardIterator());
            assertThat(batch.records()).anySatisfy(record -> {
                assertThat(record.partitionKey()).isEqualTo(scenario.entityId());
                var event = env.json.readTree(record.data().asByteArray());
                assertThat(event.path("id").asText()).isEqualTo(scenario.entityId());
                assertThat(event.path("name").asText()).isEqualTo("Acceptance example");
            });
        });
    }


    @Then("the third party receives the entity notification")
    public void notificationReceived() {
        eventually("third-party notification received").untilAsserted(() -> env.thirdParty.verifyRequest(path, expected));
    }

    @When("I request the service two greeting")
    public void requestGreeting() throws Exception {
        response = env.serviceTwo().greeting();
    }

    @Then("the greeting is {string}")
    public void greeting(String message) throws Exception {
        assertThat(env.json.readTree(response.body()).path("message").asString()).isEqualTo(message);
    }

    @When("I create an entity with a blank name")
    public void createInvalidEntity() throws Exception {
        var body = env.json.createObjectNode().put("id", scenario.entityId()).put("name", " ");
        response = env.serviceOne().create(scenario.entityId(), body.toString());
    }
}
