package dev.acceptance.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record TestScenario(String entityId) {
    public static TestScenario unique() { return new TestScenario(UUID.randomUUID().toString()); }

    public String fixture(String name) throws IOException {
        try (var input = getClass().getResourceAsStream("/fixtures/" + name)) {
            if (input == null) throw new IOException("Missing fixture " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("${entityId}", entityId);
        }
    }
}
