package dev.acceptance.support;

import java.time.Duration;
import org.awaitility.core.ConditionFactory;
import static org.awaitility.Awaitility.await;

public final class AsyncAssertions {
    private AsyncAssertions() {}

    public static ConditionFactory eventually(String description) {
        return await().alias(description).pollInterval(Duration.ofMillis(200))
                .atMost(Duration.ofSeconds(30));
    }
}
