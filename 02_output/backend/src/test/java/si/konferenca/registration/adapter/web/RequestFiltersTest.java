package si.konferenca.registration.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** SR-03 rate and size limits, SR-06 organizer HTTPS-only access. */
class RequestFiltersTest {

  /** A clock the test moves. */
  static final class MovableClock extends Clock {
    private Instant now;

    MovableClock(Instant start) {
      this.now = start;
    }

    void set(Instant instant) {
      now = instant;
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

  private static MockHttpServletRequest request(String method, String uri, String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRemoteAddr(ip);
    return request;
  }

  private static MockHttpServletResponse run(
      jakarta.servlet.Filter filter, MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  // ------------------------------------------------------------ rate limit

  private final MovableClock clock = new MovableClock(Instant.parse("2026-10-13T10:00:10.500Z"));

  private RateLimitFilter rateLimit() {
    return new RateLimitFilter(
        Map.of(
            RateLimitFilter.Group.FORM_CONFIG, 2,
            RateLimitFilter.Group.REGISTRATIONS, 1,
            RateLimitFilter.Group.EXPORT, 1),
        clock);
  }

  @Test
  void requestsAboveTheLimitInOneMinuteAreRefusedWithRetryAfter() throws Exception {
    RateLimitFilter filter = rateLimit();
    assertThat(run(filter, request("GET", "/api/form-config", "10.0.0.1")).getStatus())
        .isEqualTo(200);
    assertThat(run(filter, request("GET", "/api/form-config", "10.0.0.1")).getStatus())
        .isEqualTo(200);

    MockHttpServletResponse refused = run(filter, request("GET", "/api/form-config", "10.0.0.1"));

    assertThat(refused.getStatus()).isEqualTo(429);
    assertThat(refused.getHeader("Retry-After")).isEqualTo("50");
    assertThat(refused.getContentAsString())
        .isEqualTo("{\"message\":\"Too many requests. Please try again later.\"}");
  }

  @Test
  void limitsAreKeptPerClientAndPerGroup() throws Exception {
    RateLimitFilter filter = rateLimit();
    assertThat(run(filter, request("POST", "/api/registrations", "10.0.0.1")).getStatus())
        .isEqualTo(200);
    assertThat(run(filter, request("POST", "/api/registrations", "10.0.0.2")).getStatus())
        .isEqualTo(200);
    assertThat(run(filter, request("GET", "/api/registrations/export", "10.0.0.1")).getStatus())
        .isEqualTo(200);
    assertThat(run(filter, request("POST", "/api/registrations", "10.0.0.1")).getStatus())
        .isEqualTo(429);
    assertThat(run(filter, request("GET", "/api/registrations/export", "10.0.0.1")).getStatus())
        .isEqualTo(429);
  }

  @Test
  void aNewMinuteStartsANewWindow() throws Exception {
    RateLimitFilter filter = rateLimit();
    run(filter, request("POST", "/api/registrations", "10.0.0.1"));
    assertThat(run(filter, request("POST", "/api/registrations", "10.0.0.1")).getStatus())
        .isEqualTo(429);
    clock.set(Instant.parse("2026-10-13T10:01:00Z"));
    assertThat(run(filter, request("POST", "/api/registrations", "10.0.0.1")).getStatus())
        .isEqualTo(200);
  }

  @Test
  void otherPathsAreNotLimited() throws Exception {
    RateLimitFilter filter = rateLimit();
    for (int i = 0; i < 5; i++) {
      assertThat(run(filter, request("GET", "/api/other", "10.0.0.1")).getStatus()).isEqualTo(200);
    }
    assertThat(RateLimitFilter.group(request("GET", "/actuator/health", "x"))).isNull();
  }

  @Test
  void oldWindowsAreDroppedWhenManyClientsAreTracked() throws Exception {
    RateLimitFilter filter = rateLimit();
    run(filter, request("POST", "/api/registrations", "10.9.9.9"));
    for (int i = 0; i <= RateLimitFilter.MAX_TRACKED_CLIENTS; i++) {
      run(filter, request("GET", "/api/form-config", "10.1." + (i / 250) + "." + (i % 250)));
    }
    clock.set(Instant.parse("2026-10-13T10:01:00Z"));
    run(filter, request("GET", "/api/form-config", "10.2.0.1"));
    assertThat(run(filter, request("POST", "/api/registrations", "10.9.9.9")).getStatus())
        .isEqualTo(200);
  }

  // ------------------------------------------------------------ body size

  @Test
  void declaredLengthAboveTheLimitIsRefused() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "10.0.0.1");
    request.setContent(new byte[11]);

    MockHttpServletResponse response = run(new RequestBodyLimitFilter(10), request);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Request too large.\"}");
  }

  @Test
  void bodyAtTheLimitPasses() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "10.0.0.1");
    request.setContent(new byte[10]);
    assertThat(run(new RequestBodyLimitFilter(10), request).getStatus()).isEqualTo(200);
  }

  /** A body stream that reports no length, so only reading detects the size. */
  private static ServletInputStream stream(byte[] data) {
    java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(data);
    return new ServletInputStream() {
      @Override
      public int read() {
        return in.read();
      }

      @Override
      public boolean isFinished() {
        return in.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener listener) {
        // not used
      }
    };
  }

  private static HttpServletRequest wrapped(byte[] body, long max) throws Exception {
    MockHttpServletRequest raw =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public ServletInputStream getInputStream() {
            return stream(body);
          }
        };
    raw.setCharacterEncoding("UTF-8");
    return new RequestBodyLimitFilter.LimitedRequest(raw, max);
  }

  @Test
  void readingMoreThanTheLimitFails() throws Exception {
    ServletInputStream in = wrapped(new byte[12], 10).getInputStream();
    byte[] buffer = new byte[8];
    assertThat(in.read(buffer, 0, 8)).isEqualTo(8);
    assertThat(in.read()).isZero();
    assertThat(in.read()).isZero();
    assertThatThrownBy(in::read).isInstanceOf(RequestTooLargeException.class);
  }

  @Test
  void readingWithinTheLimitWorksThroughStreamAndReader() throws Exception {
    HttpServletRequest request = wrapped("čšž".getBytes(StandardCharsets.UTF_8), 6);
    ServletInputStream in = request.getInputStream();
    assertThat(request.getInputStream()).isSameAs(in);
    assertThat(in.isReady()).isTrue();
    assertThat(in.isFinished()).isFalse();
    in.setReadListener(null);
    try (BufferedReader reader = request.getReader()) {
      assertThat(reader.readLine()).isEqualTo("čšž");
    }
    assertThat(in.isFinished()).isTrue();
    assertThat(in.read()).isEqualTo(-1);
    assertThat(in.read(new byte[4], 0, 4)).isEqualTo(-1);
  }

  @Test
  void readerDefaultsToUtf8() throws Exception {
    MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/registrations");
    raw.setContent("ž".getBytes(StandardCharsets.UTF_8));
    try (BufferedReader reader = new RequestBodyLimitFilter.LimitedRequest(raw, 10).getReader()) {
      assertThat(reader.readLine()).isEqualTo("ž");
    }
  }

  // ------------------------------------------------------------ HTTPS-only

  @Test
  void plainHttpFromARemoteClientIsRefusedBeforeAuthentication() throws Exception {
    MockHttpServletResponse response =
        run(
            new OrganizerHttpsOnlyFilter(true),
            request("GET", "/api/registrations/export", "203.0.113.10"));
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("Organizer access requires HTTPS.");
  }

  @Test
  void httpsLocalhostOtherPathsAndDisabledCheckPass() throws Exception {
    MockHttpServletRequest secure = request("GET", "/api/registrations/export", "203.0.113.10");
    secure.setSecure(true);
    assertThat(run(new OrganizerHttpsOnlyFilter(true), secure).getStatus()).isEqualTo(200);
    assertThat(
            run(
                    new OrganizerHttpsOnlyFilter(true),
                    request("GET", "/api/registrations/export", "127.0.0.1"))
                .getStatus())
        .isEqualTo(200);
    assertThat(
            run(new OrganizerHttpsOnlyFilter(true), request("GET", "/api/form-config", "1.2.3.4"))
                .getStatus())
        .isEqualTo(200);
    assertThat(
            run(
                    new OrganizerHttpsOnlyFilter(false),
                    request("GET", "/api/registrations/export", "203.0.113.10"))
                .getStatus())
        .isEqualTo(200);
  }

  @Test
  void onlyLoopbackIpLiteralsCountAsLocal() {
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("127.0.0.1")).isTrue();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("127.1.2.3")).isTrue();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("0:0:0:0:0:0:0:1")).isTrue();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("::1")).isTrue();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("10.0.0.1")).isFalse();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("localhost")).isFalse();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("deadbeef")).isFalse();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("999.1.1.1")).isFalse();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("")).isFalse();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback(null)).isFalse();
  }
}
