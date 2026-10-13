package si.konferenca.registration.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.konferenca.registration.RegistrationProperties;

class RequestFiltersTest {

  private static MockHttpServletRequest request(String method, String path, String remote) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(remote);
    return request;
  }

  // HTTPS-only (SR-06)

  @Test
  void organizerRequestOverHttpFromRemoteClientIsRefusedWithJson() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    new HttpsOnlyFilter(true)
        .doFilter(
            request("GET", "/api/organizer/registrations.xlsx", "203.0.113.5"), response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).startsWith("application/json");
    assertThat(response.getContentAsString(StandardCharsets.UTF_8))
        .isEqualTo("{\"message\":\"HTTPS is required.\",\"fieldErrors\":{}}");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void organizerRequestPassesWhenSecureLocalOrRuleOff() throws Exception {
    MockHttpServletRequest secure = request("GET", "/api/organizer/x", "203.0.113.5");
    secure.setSecure(true);
    assertPasses(new HttpsOnlyFilter(true), secure);
    assertPasses(new HttpsOnlyFilter(true), request("GET", "/api/organizer/x", "127.0.0.1"));
    assertPasses(new HttpsOnlyFilter(true), request("GET", "/api/organizer/x", "::1"));
    assertPasses(new HttpsOnlyFilter(false), request("GET", "/api/organizer/x", "203.0.113.5"));
    assertPasses(new HttpsOnlyFilter(true), request("GET", "/api/form-config", "203.0.113.5"));
  }

  private static void assertPasses(HttpsOnlyFilter filter, MockHttpServletRequest request)
      throws Exception {
    MockFilterChain chain = new MockFilterChain();
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    assertThat(chain.getRequest()).isSameAs(request);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @ParameterizedTest
  @ValueSource(strings = {"127.0.0.1", "127.1.2.3", "::1", "0:0:0:0:0:0:0:1"})
  void loopbackAddresses(String address) {
    assertThat(HttpsOnlyFilter.isLoopback(address)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"203.0.113.5", "10.0.0.1", "::2", "localhost", "example.org", "", " "})
  void notLoopbackAddresses(String address) {
    assertThat(HttpsOnlyFilter.isLoopback(address)).isFalse();
  }

  @Test
  void nullAndMalformedAddressesAreNotLoopback() {
    assertThat(HttpsOnlyFilter.isLoopback(null)).isFalse();
    assertThat(HttpsOnlyFilter.isLoopback("::zz")).isFalse();
  }

  // Rate limits (SR-03)

  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant start) {
      this.now = start;
    }

    @Override
    public java.time.ZoneId getZone() {
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

  private static int call(RateLimitFilter filter, String path, String remote) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request("GET", path, remote), response, new MockFilterChain());
    return response.getStatus();
  }

  @Test
  void eachEndpointHasItsOwnLimitPerClientAndWindow() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-13T08:00:10Z"));
    RateLimitFilter filter =
        new RateLimitFilter(new RegistrationProperties.RateLimit(3, 2, 1), clock);

    assertThat(call(filter, "/api/form-config", "1.1.1.1")).isEqualTo(200);
    assertThat(call(filter, "/api/form-config", "1.1.1.1")).isEqualTo(200);
    assertThat(call(filter, "/api/form-config", "1.1.1.1")).isEqualTo(200);
    assertThat(call(filter, "/api/form-config", "1.1.1.1")).isEqualTo(429);
    assertThat(call(filter, "/api/form-config", "2.2.2.2")).isEqualTo(200);
    assertThat(call(filter, "/api/registrations", "1.1.1.1")).isEqualTo(200);
    assertThat(call(filter, "/api/registrations", "1.1.1.1")).isEqualTo(200);
    assertThat(call(filter, "/api/registrations", "1.1.1.1")).isEqualTo(429);
    assertThat(call(filter, "/api/organizer/registrations.xlsx", "1.1.1.1")).isEqualTo(200);
    assertThat(call(filter, "/api/organizer/registrations.xlsx", "1.1.1.1")).isEqualTo(429);
    assertThat(call(filter, "/actuator/health", "1.1.1.1")).isEqualTo(200);

    clock.now = Instant.parse("2026-10-13T08:01:00Z");
    assertThat(call(filter, "/api/form-config", "1.1.1.1")).isEqualTo(200);
    assertThat(call(filter, "/api/registrations", "1.1.1.1")).isEqualTo(200);
  }

  @Test
  void refusalCarriesRetryAfterAndJson() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-13T08:00:10.500Z"));
    RateLimitFilter filter =
        new RateLimitFilter(new RegistrationProperties.RateLimit(0, 0, 0), clock);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("GET", "/api/form-config", "1.1.1.1"), response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("50");
    assertThat(response.getContentAsString(StandardCharsets.UTF_8))
        .contains("Too many requests. Please try again later.");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void oldWindowsAreForgottenOnceManyClientsWereSeen() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-13T08:00:00Z"));
    RateLimitFilter filter =
        new RateLimitFilter(new RegistrationProperties.RateLimit(1, 1, 1), clock);
    for (int i = 0; i <= RateLimitFilter.CLEANUP_THRESHOLD; i++) {
      call(filter, "/api/form-config", "10.0." + (i / 256) + "." + (i % 256));
    }
    assertThat(call(filter, "/api/form-config", "10.0.0.0")).isEqualTo(429);

    clock.now = Instant.parse("2026-10-13T08:01:00Z");
    assertThat(call(filter, "/api/form-config", "10.0.0.0")).isEqualTo(200);
    assertThat(call(filter, "/api/form-config", "10.0.0.0")).isEqualTo(429);
  }

  @Test
  void bucketsByPath() {
    assertThat(RateLimitFilter.bucket("/api/form-config")).isEqualTo("form-config");
    assertThat(RateLimitFilter.bucket("/api/registrations")).isEqualTo("registrations");
    assertThat(RateLimitFilter.bucket("/api/organizer/registrations.xlsx")).isEqualTo("export");
    assertThat(RateLimitFilter.bucket("/api/registrations/x")).isNull();
    assertThat(RateLimitFilter.bucket("/api/organizer")).isNull();
  }

  // Body size (SR-03)

  @Test
  void declaredOversizeBodyIsRefused() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "1.1.1.1");
    request.setContent(new byte[11]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    new BodySizeLimitFilter(10).doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString(StandardCharsets.UTF_8))
        .contains("The request is too large.");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void declaredBodyAtTheLimitPassesUnchanged() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "1.1.1.1");
    request.setContent(new byte[10]);
    MockFilterChain chain = new MockFilterChain();

    new BodySizeLimitFilter(10).doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isSameAs(request);
  }

  /** A request without Content-Length (chunked). */
  private static HttpServletRequest chunked(byte[] body) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(body);
    request.setCharacterEncoding("UTF-8");
    return request;
  }

  @Test
  void undeclaredOversizeBodyIsRefused() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    new BodySizeLimitFilter(10).doFilter(chunked(new byte[11]), response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void undeclaredSmallBodyIsReplayedToTheApplication() throws Exception {
    AtomicReference<HttpServletRequest> seen = new AtomicReference<>();
    byte[] body = "čšž{}".getBytes(StandardCharsets.UTF_8);

    new BodySizeLimitFilter(64)
        .doFilter(
            chunked(body),
            new MockHttpServletResponse(),
            (req, res) -> seen.set((HttpServletRequest) req));

    HttpServletRequest replay = seen.get();
    assertThat(replay.getContentLength()).isEqualTo(body.length);
    assertThat(replay.getContentLengthLong()).isEqualTo(body.length);
    assertThat(replay.getInputStream().readAllBytes()).isEqualTo(body);
    try (BufferedReader reader = replay.getReader()) {
      assertThat(reader.readLine()).isEqualTo("čšž{}");
    }
    var stream = replay.getInputStream();
    assertThat(stream.isReady()).isTrue();
    assertThat(stream.isFinished()).isFalse();
    byte[] buffer = new byte[body.length];
    assertThat(stream.read(buffer, 0, buffer.length)).isEqualTo(body.length);
    assertThat(stream.isFinished()).isTrue();
    assertThat(stream.read()).isEqualTo(-1);
  }

  @Test
  void replayReaderDefaultsToUtf8() throws Exception {
    MockHttpServletRequest original = new MockHttpServletRequest();
    byte[] body = "ž".getBytes(StandardCharsets.UTF_8);
    var replay = new BodySizeLimitFilter.CachedBodyRequest(original, body);
    assertThat(new String(new ByteArrayInputStream(body).readAllBytes(), StandardCharsets.UTF_8))
        .isEqualTo("ž");
    try (BufferedReader reader = replay.getReader()) {
      assertThat(reader.readLine()).isEqualTo("ž");
    }
  }
}
