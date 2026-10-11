package si.konferenca.registration.api;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client counters in one-minute windows that start at the first request of the client in the
 * group (SR-03). Expired windows are evicted.
 */
public final class FixedWindowRateLimiter {

  private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
  private static final int EVICT_ABOVE = 10_000;

  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public FixedWindowRateLimiter(Clock clock) {
    this.clock = clock;
  }

  /** Returns 0 when the request is allowed, otherwise the seconds until the window resets. */
  public long acquire(String key, int limit) {
    long now = clock.millis();
    if (windows.size() > EVICT_ABOVE) {
      windows.values().removeIf(w -> now - w.start() >= WINDOW_MILLIS);
    }
    Window w =
        windows.compute(
            key,
            (k, old) ->
                old == null || now - old.start() >= WINDOW_MILLIS
                    ? new Window(now, 1)
                    : new Window(old.start(), old.count() + 1));
    if (w.count() <= limit) {
      return 0;
    }
    return Math.max(1, (w.start() + WINDOW_MILLIS - now + 999) / 1000);
  }

  /** Number of clients currently tracked (memory bound check). */
  int trackedKeys() {
    return windows.size();
  }

  private record Window(long start, int count) {}
}
