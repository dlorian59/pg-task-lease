package io.github.alex59.pgtasklease;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidationTest {
    @Test void rejectsInvalidRequests() {
        assertThrows(IllegalArgumentException.class,
                () -> new TaskRequest(UUID.randomUUID(), " ", "", 0, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new TaskRequest(UUID.randomUUID(), "q", "", 0, 0, Duration.ZERO));
        assertThrows(NullPointerException.class,
                () -> new TaskRequest(UUID.randomUUID(), "q", null, 0, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new TaskRequest(UUID.randomUUID(), "q", "", 0, 1, Duration.ofNanos(1)));
    }

    @Test void requestCopiesPreserveIdentityAndValidateOverrides() {
        var original = TaskRequest.ready(UUID.randomUUID(), "reports", "2026-Q4");
        var changed = original.withPriority(10).withMaxAttempts(2).withDelay(Duration.ofSeconds(2));
        assertEquals(original.id(), changed.id());
        assertEquals(original.payload(), changed.payload());
        assertEquals(Duration.ZERO, original.delay());
        assertEquals(Duration.ofSeconds(2), changed.delay());
        assertThrows(IllegalArgumentException.class, () -> original.withMaxAttempts(0));
        assertThrows(IllegalArgumentException.class, () -> original.withDelay(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> original.withDelay(Duration.ofSeconds(Long.MAX_VALUE)));
    }

    @Test void rejectsInvalidDurations() {
        assertThrows(IllegalArgumentException.class, () -> Checks.millis(Duration.ZERO, "lease", true));
        assertThrows(IllegalArgumentException.class, () -> Checks.millis(Duration.ofMillis(-1), "delay", false));
        assertThrows(IllegalArgumentException.class, () -> Checks.millis(Duration.ofDays(366), "lease", true));
        assertEquals(1, Checks.millis(Duration.ofMillis(1), "lease", true));
    }
}
