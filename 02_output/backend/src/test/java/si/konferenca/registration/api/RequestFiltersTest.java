package si.konferenca.registration.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestFiltersTest {

  /** A clock the test moves forward. */
  static final class MutableClock extends Clock {
    Instant now = Instant.parse("2026-10-11T10:00:00Z");

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  @Test
  void limiterAllowsUpToTheLimitPerKeyAndResetsAfterAMinute() {
    MutableClock clock = new MutableClock();
    FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock);

    assertThat(limiter.acquire("a", 2)).isZero();
    assertThat(limiter.acquire("a", 2)).isZero();
    assertThat(limiter.acquire("b", 2)).isZero();
    clock.now = clock.now.plusSeconds(20);
    assertThat(limiter.acquire("a", 2)).isEqualTo(40);
    clock.now = clock.now.plusMillis(39_500);
    assertThat(limiter.acquire("a", 2)).isEqualTo(1);
    clock.now = clock.now.plusMillis(500);
    assertThat(limiter.acquire("a", 2)).isZero();
  }

  @Test
  void limiterEvictsExpiredWindowsWhenLarge() {
    MutableClock clock = new MutableClock();
    FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock);
    for (int i = 0; i <= 10_001; i++) {
      limiter.acquire("k" + i, 1);
    }
    clock.now = clock.now.plusSeconds(61);

    assertThat(limiter.acquire("k1", 1)).isZero();
    assertThat(limiter.acquire("k1", 1)).isPositive();
  }

  private static RequestLimitFilter filter(int registrations, int max) {
    return new RequestLimitFilter(
        new FixedWindowRateLimiter(Clock.systemUTC()),
        new RequestLimitFilter.Limits(registrations, 1, 1, max));
  }

  private static MockHttpServletRequest post(String body) {
    MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/registrations");
    r.setContent(body.getBytes(StandardCharsets.UTF_8));
    r.setContentType("application/json");
    return r;
  }

  @Test
  void passesTheBodyOnAndLimitsPerClient() throws Exception {
    RequestLimitFilter f = filter(1, 100);
    AtomicReference<String> seen = new AtomicReference<>();
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
              throws java.io.IOException {
            seen.set(((HttpServletRequest) req).getReader().readLine());
          }
        };
    MockHttpServletResponse ok = new MockHttpServletResponse();
    f.doFilter(post("{\"a\":1}"), ok, chain);
    assertThat(ok.getStatus()).isEqualTo(200);
    assertThat(seen.get()).isEqualTo("{\"a\":1}");

    MockHttpServletResponse limited = new MockHttpServletResponse();
    f.doFilter(post("{}"), limited, new MockFilterChain());
    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isNotBlank();
    assertThat(limited.getContentAsString()).contains("\"fieldErrors\":[]");

    MockHttpServletRequest other = post("{}");
    other.setRemoteAddr("10.0.0.9");
    MockHttpServletResponse otherResponse = new MockHttpServletResponse();
    f.doFilter(other, otherResponse, new MockFilterChain());
    assertThat(otherResponse.getStatus()).isEqualTo(200);
  }

  @Test
  void refusesBodiesAboveTheLimitWithOrWithoutContentLength() throws Exception {
    RequestLimitFilter f = filter(100, 5);
    MockHttpServletResponse exact = new MockHttpServletResponse();
    f.doFilter(post("12345"), exact, new MockFilterChain());
    assertThat(exact.getStatus()).isEqualTo(200);

    MockHttpServletResponse declared = new MockHttpServletResponse();
    f.doFilter(post("123456"), declared, new MockFilterChain());
    assertThat(declared.getStatus()).isEqualTo(413);

    MockHttpServletRequest chunked =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    chunked.setContent("1234567".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse counted = new MockHttpServletResponse();
    f.doFilter(chunked, counted, new MockFilterChain());
    assertThat(counted.getStatus()).isEqualTo(413);
  }

  @Test
  void eachGroupHasItsOwnLimitAndOtherPathsAreNotLimited() throws Exception {
    RequestLimitFilter f = filter(5, 100);
    for (String path : new String[] {"/api/form-config", "/api/export"}) {
      MockHttpServletResponse first = new MockHttpServletResponse();
      f.doFilter(new MockHttpServletRequest("GET", path), first, new MockFilterChain());
      MockHttpServletResponse second = new MockHttpServletResponse();
      f.doFilter(new MockHttpServletRequest("GET", path), second, new MockFilterChain());
      assertThat(first.getStatus()).isEqualTo(200);
      assertThat(second.getStatus()).isEqualTo(429);
    }
    for (String path : new String[] {"/api/other", "/actuator/health"}) {
      for (int i = 0; i < 3; i++) {
        MockHttpServletResponse r = new MockHttpServletResponse();
        f.doFilter(new MockHttpServletRequest("GET", path), r, new MockFilterChain());
        assertThat(r.getStatus()).isEqualTo(200);
      }
    }
  }

  @Test
  void transportGuardRefusesPlainHttpFromRemoteClients() throws Exception {
    OrganizerTransportGuard guard = new OrganizerTransportGuard(true);

    MockHttpServletRequest remote = new MockHttpServletRequest("GET", "/api/export");
    remote.setRemoteAddr("203.0.113.7");
    MockHttpServletResponse refused = new MockHttpServletResponse();
    guard.doFilter(remote, refused, new MockFilterChain());
    assertThat(refused.getStatus()).isEqualTo(403);

    remote.setSecure(true);
    MockHttpServletResponse secure = new MockHttpServletResponse();
    guard.doFilter(remote, secure, new MockFilterChain());
    assertThat(secure.getStatus()).isEqualTo(200);

    MockHttpServletRequest local = new MockHttpServletRequest("GET", "/api/export");
    local.setRemoteAddr("::1");
    MockHttpServletResponse loopback = new MockHttpServletResponse();
    guard.doFilter(local, loopback, new MockFilterChain());
    assertThat(loopback.getStatus()).isEqualTo(200);

    MockHttpServletRequest form = new MockHttpServletRequest("GET", "/api/form-config");
    form.setRemoteAddr("203.0.113.7");
    MockHttpServletResponse other = new MockHttpServletResponse();
    guard.doFilter(form, other, new MockFilterChain());
    assertThat(other.getStatus()).isEqualTo(200);

    MockHttpServletResponse off = new MockHttpServletResponse();
    MockHttpServletRequest plain = new MockHttpServletRequest("GET", "/api/export");
    plain.setRemoteAddr("203.0.113.7");
    new OrganizerTransportGuard(false).doFilter(plain, off, new MockFilterChain());
    assertThat(off.getStatus()).isEqualTo(200);
  }

  @Test
  void loopbackDetectionNeverResolvesNames() {
    assertThat(OrganizerTransportGuard.isLoopback("127.0.0.1")).isTrue();
    assertThat(OrganizerTransportGuard.isLoopback("127.5.5.5")).isTrue();
    assertThat(OrganizerTransportGuard.isLoopback("10.0.0.1")).isFalse();
    assertThat(OrganizerTransportGuard.isLoopback("localhost")).isFalse();
    assertThat(OrganizerTransportGuard.isLoopback(null)).isFalse();
    assertThat(OrganizerTransportGuard.isLoopback("1:2:3")).isFalse();
  }

  @Test
  void errorWriterEscapesTheMessage() throws Exception {
    MockHttpServletResponse r = new MockHttpServletResponse();

    ErrorWriter.write(r, 418, "a \"quoted\" \\ text");

    assertThat(r.getStatus()).isEqualTo(418);
    assertThat(r.getContentType()).startsWith("application/json");
    assertThat(r.getContentAsString())
        .isEqualTo("{\"message\":\"a \\\"quoted\\\" \\\\ text\",\"fieldErrors\":[]}");
  }
}
