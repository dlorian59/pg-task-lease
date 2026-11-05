package io.github.alex59.pgtasklease;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * A task to submit. Reusing its id makes retries of submission deduplicate.
 * @param id globally unique task id, retained for retries of submission
 * @param queue logical queue name, 1..120 characters
 * @param payload opaque non-null content
 * @param priority larger numbers are preferred at claim time
 * @param maxAttempts positive maximum number of claims
 * @param delay initial availability delay from database time
 */
public record TaskRequest(UUID id, String queue, String payload, int priority,
                          int maxAttempts, Duration delay) {
    /** Default claim budget used by {@link #ready(UUID, String, String)}. */
    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * Creates an immediately available task at normal priority, with five attempts.
     * Keep the supplied id stable when retrying submission; no UUID is generated implicitly.
     * @param id durable application submission key
     * @param queue logical queue
     * @param payload opaque business content
     * @return validated task request
     */
    public static TaskRequest ready(UUID id, String queue, String payload) {
        return new TaskRequest(id, queue, payload, 0, DEFAULT_MAX_ATTEMPTS, Duration.ZERO);
    }

    /**
     * @param value larger numbers are preferred
     * @return a new validated request
     */
    public TaskRequest withPriority(int value) {
        return new TaskRequest(id, queue, payload, value, maxAttempts, delay);
    }

    /**
     * @param value positive maximum claims
     * @return a new validated request
     */
    public TaskRequest withMaxAttempts(int value) {
        return new TaskRequest(id, queue, payload, priority, value, delay);
    }

    /**
     * @param value initial delay from database time
     * @return a new validated request
     */
    public TaskRequest withDelay(Duration value) {
        return new TaskRequest(id, queue, payload, priority, maxAttempts, value);
    }

    /**
     * Validates the request before any database operation.
     * @param id globally unique task id
     * @param queue logical queue name
     * @param payload opaque non-null content
     * @param priority scheduling preference
     * @param maxAttempts positive claim budget
     * @param delay initial availability delay
     */
    public TaskRequest {
        Objects.requireNonNull(id, "id");
        Checks.text(queue, "queue", 120);
        Objects.requireNonNull(payload, "payload");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
        Checks.millis(delay, "delay", false);
    }
}
