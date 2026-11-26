package pl.elektrodemo.copilot.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Fixed one-hour window per (bucket, ip), kept in memory. Stale windows are swept every 256 calls. */
@Component
public class RateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);
    private static final long CLEANUP_EVERY = 256;

    private final Clock clock;
    private final ConcurrentHashMap<Key, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String bucket, String ip, int limitPerHour) {
        Instant now = clock.instant();
        if (calls.incrementAndGet() % CLEANUP_EVERY == 0) {
            windows.entrySet().removeIf(e -> isExpired(e.getValue(), now));
        }
        boolean[] allowed = new boolean[1];
        windows.compute(new Key(bucket, ip), (k, w) -> {
            Window current = (w == null || isExpired(w, now)) ? new Window(now) : w;
            if (current.count < limitPerHour) {
                current.count++;
                allowed[0] = true;
            }
            return current;
        });
        return allowed[0];
    }

    int size() {
        return windows.size();
    }

    private static boolean isExpired(Window w, Instant now) {
        return !now.isBefore(w.start.plus(WINDOW));
    }

    private record Key(String bucket, String ip) {
    }

    private static final class Window {
        private final Instant start;
        private int count;

        Window(Instant start) {
            this.start = start;
        }
    }
}
