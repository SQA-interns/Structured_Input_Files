package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestFiltersTest {

  // ---- rate limit ---------------------------------------------------------------------------

  private static final class MovableClock extends Clock {
    private Instant now = Instant.parse("2026-10-12T10:00:00Z");

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

  private static MockHttpServletRequest request(String method, String uri, String client) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRemoteAddr(client);
    return request;
  }

  @Test
  void rateLimitAllowsUpToTheLimitPerClientAndEndpointInEachMinute() throws Exception {
    MovableClock clock = new MovableClock();
    RateLimitFilter filter = new RateLimitFilter(WebTestSupport.properties(true, 2, 100), clock);

    assertThat(filter.allow("/api/form-config", "a")).isTrue();
    assertThat(filter.allow("/api/form-config", "a")).isTrue();
    assertThat(filter.allow("/api/form-config", "a")).isFalse();
    assertThat(filter.allow("/api/form-config", "b")).isTrue();
    assertThat(filter.allow("/api/registrations", "a")).isTrue();
    assertThat(filter.allow("/api/registrations", "a")).isTrue();
    assertThat(filter.allow("/api/registrations", "a")).isTrue();
    assertThat(filter.allow("/api/registrations", "a")).isFalse();
    assertThat(filter.allow("/api/export", "a")).isTrue();

    clock.now = clock.now.plusSeconds(60);
    assertThat(filter.allow("/api/form-config", "a")).isTrue();
  }

  @Test
  void rateLimitAnswers429WithRetryAfter() throws Exception {
    RateLimitFilter filter =
        new RateLimitFilter(WebTestSupport.properties(true, 1, 100), new MovableClock());
    MockFilterChain first = new MockFilterChain();
    filter.doFilter(request("GET", "/api/form-config", "c"), new MockHttpServletResponse(), first);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain second = new MockFilterChain();

    filter.doFilter(request("GET", "/api/form-config", "c"), response, second);

    assertThat(first.getRequest()).isNotNull();
    assertThat(second.getRequest()).isNull();
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    assertThat(response.getContentAsString())
        .isEqualTo("{\"message\":\"" + RateLimitFilter.MESSAGE + "\"}");
    assertThat(response.getContentType()).startsWith("application/json");
  }

  @Test
  void rateLimitIgnoresOtherPaths() throws Exception {
    RateLimitFilter filter =
        new RateLimitFilter(WebTestSupport.properties(true, 0, 100), new MovableClock());
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("GET", "/actuator/health", "c"), new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void rateLimitForgetsOldWindowsWhenTrackingManyClients() throws Exception {
    MovableClock clock = new MovableClock();
    RateLimitFilter filter = new RateLimitFilter(WebTestSupport.properties(true, 1, 100), clock);
    for (int i = 0; i <= 10_001; i++) {
      filter.allow("/api/form-config", "client-" + i);
    }
    clock.now = clock.now.plusSeconds(60);

    assertThat(filter.allow("/api/form-config", "client-0")).isTrue();
    assertThat(filter.allow("/api/form-config", "client-0")).isFalse();
  }

  @Test
  void rateLimitUsesTheSystemClockByDefault() {
    RateLimitFilter filter = new RateLimitFilter(WebTestSupport.properties(true, 1, 100));

    assertThat(filter.allow("/api/export", "x")).isTrue();
  }

  // ---- body size ----------------------------------------------------------------------------

  private static MockHttpServletRequest post(byte[] body, boolean withLength) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return withLength ? body.length : -1;
          }
        };
    request.setContent(body);
    request.setCharacterEncoding("UTF-8");
    return request;
  }

  @Test
  void bodySizeRejectsADeclaredLengthAboveTheLimit() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(WebTestSupport.properties(true, 1, 10));
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(post(new byte[11], true), response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).contains(BodySizeLimitFilter.MESSAGE);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void bodySizeRejectsAnUndeclaredLengthAboveTheLimit() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(WebTestSupport.properties(true, 1, 10));
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(post(new byte[11], false), response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void bodySizePassesABodyAtTheLimitReadableAsStreamAndReader() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(WebTestSupport.properties(true, 1, 10));
    byte[] body = "{\"a\":\"čš\"}".getBytes(StandardCharsets.UTF_8);
    assertThat(body).hasSize(12);
    filter = new BodySizeLimitFilter(WebTestSupport.properties(true, 1, 12));
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(post(body, false), new MockHttpServletResponse(), chain);

    HttpServletRequest passed = (HttpServletRequest) chain.getRequest();
    assertThat(passed.getContentLength()).isEqualTo(12);
    assertThat(passed.getContentLengthLong()).isEqualTo(12);
    ServletInputStream in = passed.getInputStream();
    assertThat(in.isReady()).isTrue();
    assertThat(in.isFinished()).isFalse();
    byte[] first = new byte[2];
    assertThat(in.read(first, 0, 2)).isEqualTo(2);
    assertThat(in.read()).isEqualTo('a');
    assertThat(in.readAllBytes()).hasSize(9);
    assertThat(in.isFinished()).isTrue();
    assertThatThrownBy(() -> in.setReadListener(null))
        .isInstanceOf(UnsupportedOperationException.class);
    try (BufferedReader reader = passed.getReader()) {
      assertThat(reader.readLine()).isEqualTo("{\"a\":\"čš\"}");
    }
  }

  @Test
  void bodySizeReaderDefaultsToUtf8() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(WebTestSupport.properties(true, 1, 100));
    MockHttpServletRequest request = post("ž".getBytes(StandardCharsets.UTF_8), true);
    request.setCharacterEncoding((String) null);
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    try (BufferedReader reader = ((HttpServletRequest) chain.getRequest()).getReader()) {
      assertThat(reader.readLine()).isEqualTo("ž");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET /api/form-config", "HEAD /api/export", "POST /actuator/x"})
  void bodySizeIgnoresRequestsWithoutApiBodies(String line) throws Exception {
    String[] parts = line.split(" ");
    BodySizeLimitFilter filter = new BodySizeLimitFilter(WebTestSupport.properties(true, 1, 0));
    MockHttpServletRequest request = new MockHttpServletRequest(parts[0], parts[1]);
    request.setContent(new byte[5]);
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isSameAs(request);
  }

  @Test
  void bodySizeClosesTheOriginalStream() throws Exception {
    boolean[] closed = {false};
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public ServletInputStream getInputStream() {
            InputStream in = new ByteArrayInputStream(new byte[] {1});
            return new ServletInputStream() {
              @Override
              public int read() throws IOException {
                return in.read();
              }

              @Override
              public void close() {
                closed[0] = true;
              }

              @Override
              public boolean isFinished() {
                return false;
              }

              @Override
              public boolean isReady() {
                return true;
              }

              @Override
              public void setReadListener(jakarta.servlet.ReadListener listener) {}
            };
          }
        };

    new BodySizeLimitFilter(WebTestSupport.properties(true, 1, 10))
        .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(closed[0]).isTrue();
  }

  // ---- HTTPS-only organizer access ----------------------------------------------------------

  @ParameterizedTest
  @ValueSource(strings = {"127.0.0.1", "127.1.2.3", "::1", "0:0:0:0:0:0:0:1"})
  void loopbackAddresses(String address) {
    assertThat(HttpsOnlyOrganizerFilter.isLoopback(address)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"203.0.113.10", "10.0.0.1", "::2", "localhost", "127.0.0.1 ", "[::1]", ""})
  void nonLoopbackAddresses(String address) {
    assertThat(HttpsOnlyOrganizerFilter.isLoopback(address)).isFalse();
  }

  @Test
  void nullAddressIsNotLoopback() {
    assertThat(HttpsOnlyOrganizerFilter.isLoopback(null)).isFalse();
  }

  @Test
  void httpsOnlyRefusesPlainHttpFromRemoteClients() throws Exception {
    HttpsOnlyOrganizerFilter filter =
        new HttpsOnlyOrganizerFilter(WebTestSupport.properties(true, 1, 10));
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("GET", "/api/export", "203.0.113.10"), response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains(HttpsOnlyOrganizerFilter.MESSAGE);
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void httpsOnlyPassesSecureLocalAndOtherRequests() throws Exception {
    HttpsOnlyOrganizerFilter filter =
        new HttpsOnlyOrganizerFilter(WebTestSupport.properties(true, 1, 10));
    MockHttpServletRequest secure = request("GET", "/api/export", "203.0.113.10");
    secure.setSecure(true);
    MockHttpServletRequest local = request("GET", "/api/export", "127.0.0.1");
    MockHttpServletRequest other = request("GET", "/api/form-config", "203.0.113.10");

    for (MockHttpServletRequest request : new MockHttpServletRequest[] {secure, local, other}) {
      MockFilterChain chain = new MockFilterChain();
      filter.doFilter(request, new MockHttpServletResponse(), chain);
      assertThat(chain.getRequest()).isSameAs(request);
    }
  }

  @Test
  void httpsOnlyCanBeSwitchedOff() throws Exception {
    HttpsOnlyOrganizerFilter filter =
        new HttpsOnlyOrganizerFilter(WebTestSupport.properties(false, 1, 10));
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(
        request("GET", "/api/export", "203.0.113.10"), new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isNotNull();
  }
}
