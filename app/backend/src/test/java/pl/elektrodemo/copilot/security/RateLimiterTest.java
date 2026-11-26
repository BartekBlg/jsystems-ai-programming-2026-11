package pl.elektrodemo.copilot.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    /** Mutable clock so tests can advance time without sleeping. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static final Instant START = Instant.parse("2026-11-26T10:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void allowsTenPerHourAndRejectsTheEleventh() {
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire("cases", "198.51.100.7", 10)).isTrue();
        }

        assertThat(limiter.tryAcquire("cases", "198.51.100.7", 10)).isFalse();
    }

    @Test
    void differentIpsAreIndependent() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("cases", "198.51.100.7", 10);
        }

        assertThat(limiter.tryAcquire("cases", "198.51.100.8", 10)).isTrue();
    }

    @Test
    void differentBucketsAreIndependent() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("cases", "198.51.100.7", 10);
        }

        assertThat(limiter.tryAcquire("chat", "198.51.100.7", 10)).isTrue();
    }

    @Test
    void allowedAgainAfterOneHour() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("cases", "198.51.100.7", 10);
        }
        assertThat(limiter.tryAcquire("cases", "198.51.100.7", 10)).isFalse();

        clock.advance(Duration.ofHours(1));

        assertThat(limiter.tryAcquire("cases", "198.51.100.7", 10)).isTrue();
    }

    @Test
    void windowIsFixedFromFirstRequestNotSliding() {
        assertThat(limiter.tryAcquire("chat", "203.0.113.5", 2)).isTrue();
        clock.advance(Duration.ofMinutes(30));
        assertThat(limiter.tryAcquire("chat", "203.0.113.5", 2)).isTrue();
        clock.advance(Duration.ofMinutes(29));
        assertThat(limiter.tryAcquire("chat", "203.0.113.5", 2)).isFalse();
        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("chat", "203.0.113.5", 2)).isTrue();
    }

    @Test
    void staleWindowsAreCleanedUp() {
        for (int i = 0; i < 300; i++) {
            limiter.tryAcquire("cases", "10.0.0." + i, 10);
        }
        assertThat(limiter.size()).isEqualTo(300);

        clock.advance(Duration.ofHours(2));
        for (int i = 0; i < 256; i++) {
            limiter.tryAcquire("cases", "192.0.2.1", 10);
        }

        assertThat(limiter.size()).isLessThanOrEqualTo(2);
    }
}
