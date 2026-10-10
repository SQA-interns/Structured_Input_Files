package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.konferenca.registration.api.PayloadTooLargeException;

class RequestFiltersTest {

  private final AtomicLong clock = new AtomicLong();

  private FixedWindowRateLimiter limiter(int max) {
    return new FixedWindowRateLimiter(max, Duration.ofSeconds(60), clock::get);
  }

  @Test
  void limiterAllowsUpToTheLimitPerKeyAndResetsAfterTheWindow() {
    FixedWindowRateLimiter limiter = limiter(2);

    assertThat(limiter.acquire("a")).isZero();
    assertThat(limiter.acquire("a")).isZero();
    clock.set(Duration.ofSeconds(15).toNanos());
    assertThat(limiter.acquire("a")).isEqualTo(45);
    assertThat(limiter.acquire("b")).isZero();
    clock.set(Duration.ofSeconds(59).toNanos() + 900_000_000L);
    assertThat(limiter.acquire("a")).isEqualTo(1);
    clock.set(Duration.ofSeconds(60).toNanos());
    assertThat(limiter.acquire("a")).isZero();
  }

  @Test
  void limiterCleansUpExpiredWindowsWhenLarge() {
    FixedWindowRateLimiter limiter = limiter(1);
    for (int i = 0; i <= 10_001; i++) {
      limiter.acquire("k" + i);
    }
    clock.set(Duration.ofSeconds(61).toNanos());

    assertThat(limiter.acquire("k0")).isZero();
    assertThat(limiter.acquire("k0")).isPositive();
  }

  private static MockHttpServletResponse run(
      jakarta.servlet.Filter filter, MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  @Test
  void rateLimitFilterChoosesTheBucketByPath() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(limiter(1), limiter(1), limiter(3));
    MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/registrations");
    MockHttpServletRequest export =
        new MockHttpServletRequest("GET", "/api/export/registrations.xlsx");
    MockHttpServletRequest other = new MockHttpServletRequest("GET", "/api/form-config");
    MockHttpServletRequest getRegistrations =
        new MockHttpServletRequest("GET", "/api/registrations");

    assertThat(run(filter, post).getStatus()).isEqualTo(200);
    MockHttpServletResponse limited = run(filter, post);
    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
    assertThat(limited.getContentAsString()).contains("\"error\":\"TOO_MANY_REQUESTS\"");
    assertThat(run(filter, export).getStatus()).isEqualTo(200);
    assertThat(run(filter, export).getStatus()).isEqualTo(429);
    assertThat(run(filter, other).getStatus()).isEqualTo(200);
    assertThat(run(filter, getRegistrations).getStatus()).isEqualTo(200);
    assertThat(run(filter, other).getStatus()).isEqualTo(200);
    assertThat(run(filter, other).getStatus()).isEqualTo(429);
    MockHttpServletRequest health = new MockHttpServletRequest("GET", "/actuator/health");
    for (int i = 0; i < 5; i++) {
      assertThat(run(filter, health).getStatus()).isEqualTo(200);
    }
  }

  @Test
  void httpsOnlyFilterRefusesPlainRemoteExportOnly() throws Exception {
    HttpsOnlyFilter filter = new HttpsOnlyFilter(true);
    MockHttpServletRequest remote =
        new MockHttpServletRequest("GET", "/api/export/registrations.xlsx");
    remote.setRemoteAddr("203.0.113.9");
    MockHttpServletRequest secure =
        new MockHttpServletRequest("GET", "/api/export/registrations.xlsx");
    secure.setRemoteAddr("203.0.113.9");
    secure.setSecure(true);
    MockHttpServletRequest local =
        new MockHttpServletRequest("GET", "/api/export/registrations.xlsx");
    local.setRemoteAddr("::1");
    MockHttpServletRequest form = new MockHttpServletRequest("GET", "/api/form-config");
    form.setRemoteAddr("203.0.113.9");

    MockHttpServletResponse refused = run(filter, remote);
    assertThat(refused.getStatus()).isEqualTo(403);
    assertThat(refused.getContentAsString()).contains("HTTPS_REQUIRED");
    assertThat(run(filter, secure).getStatus()).isEqualTo(200);
    assertThat(run(filter, local).getStatus()).isEqualTo(200);
    assertThat(run(filter, form).getStatus()).isEqualTo(200);
    assertThat(run(new HttpsOnlyFilter(false), remote).getStatus()).isEqualTo(200);
  }

  @Test
  void loopbackDetection() {
    assertThat(HttpsOnlyFilter.loopback("127.0.0.1")).isTrue();
    assertThat(HttpsOnlyFilter.loopback("127.10.0.2")).isTrue();
    assertThat(HttpsOnlyFilter.loopback("0:0:0:0:0:0:0:1")).isTrue();
    assertThat(HttpsOnlyFilter.loopback("10.0.0.1")).isFalse();
    assertThat(HttpsOnlyFilter.loopback("localhost")).isFalse();
    assertThat(HttpsOnlyFilter.loopback(null)).isFalse();
    assertThat(HttpsOnlyFilter.loopback("1:2:3")).isFalse();
  }

  @Test
  void sizeFilterRefusesDeclaredAndStreamedOversizedBodies() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);
    MockHttpServletRequest declared = new MockHttpServletRequest("POST", "/api/registrations");
    declared.setContent(new byte[11]);

    MockHttpServletResponse refused = run(filter, declared);
    assertThat(refused.getStatus()).isEqualTo(413);
    assertThat(refused.getContentAsString()).contains("PAYLOAD_TOO_LARGE");

    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();
    MockHttpServletRequest streamed =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    streamed.setContent(new byte[11]);
    filter.doFilter(
        streamed,
        new MockHttpServletResponse(),
        (req, res) -> passed.set((HttpServletRequest) req));
    var in = passed.get().getInputStream();
    assertThat(passed.get().getInputStream()).isSameAs(in);
    assertThat(in.read(new byte[5], 0, 5)).isEqualTo(5);
    assertThat(in.read()).isZero();
    assertThat(in.isFinished()).isFalse();
    assertThat(in.isReady()).isTrue();
    assertThatThrownBy(() -> in.read(new byte[10], 0, 10))
        .isInstanceOf(PayloadTooLargeException.class);
  }

  @Test
  void sizeFilterLetsSmallBodiesThrough() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);
    MockHttpServletRequest small = new MockHttpServletRequest("POST", "/api/registrations");
    small.setContent(new byte[10]);
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();

    filter.doFilter(
        small, new MockHttpServletResponse(), (req, res) -> passed.set((HttpServletRequest) req));

    assertThat(passed.get().getInputStream().readAllBytes()).hasSize(10);
    assertThat(passed.get().getInputStream().read()).isEqualTo(-1);
  }
}
