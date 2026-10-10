package si.konferenca.registration.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** In-memory fixed-window counter per key (SR-03, SB-06); one backend instance is assumed. */
final class FixedWindowRateLimiter {

  private static final int CLEANUP_THRESHOLD = 10_000;

  private final int maxRequests;
  private final long windowNanos;
  private final LongSupplier nanoTime;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  FixedWindowRateLimiter(int maxRequests, Duration window, LongSupplier nanoTime) {
    this.maxRequests = maxRequests;
    this.windowNanos = window.toNanos();
    this.nanoTime = nanoTime;
  }

  /**
   * Counts a request for the key.
   *
   * @return 0 when allowed, otherwise the seconds until the window ends (at least 1)
   */
  long acquire(String key) {
    long now = nanoTime.getAsLong();
    if (windows.size() > CLEANUP_THRESHOLD) {
      windows.values().removeIf(w -> now - w.start >= windowNanos);
    }
    Window window =
        windows.compute(
            key,
            (k, w) -> w == null || now - w.start >= windowNanos ? new Window(now, 1) : w.next());
    if (window.count <= maxRequests) {
      return 0;
    }
    long remaining = windowNanos - (now - window.start);
    return Math.max(1, Duration.ofNanos(remaining).toSeconds());
  }

  private record Window(long start, int count) {
    Window next() {
      return new Window(start, count == Integer.MAX_VALUE ? count : count + 1);
    }
  }
}
