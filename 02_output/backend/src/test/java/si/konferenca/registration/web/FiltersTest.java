package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FiltersTest {

  private final List<HttpServletRequest> passed = new ArrayList<>();
  private final FilterChain chain = (req, res) -> passed.add((HttpServletRequest) req);

  private static MockHttpServletRequest request(String method, String path, String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(ip);
    return request;
  }

  /** A clock the test can move. */
  static final class MutableClock extends Clock {
    Instant now = Instant.parse("2026-10-12T10:00:00Z");

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

  @Test
  void rateLimitAllowsTheLimitPerGroupAndIpThenAnswers429() throws Exception {
    MutableClock clock = new MutableClock();
    RateLimitFilter filter = new RateLimitFilter(new RateLimitFilter.Limits(2, 3, 1), clock);

    for (int i = 0; i < 2; i++) {
      filter.doFilter(
          request("POST", "/api/registrations", "1.1.1.1"), new MockHttpServletResponse(), chain);
    }
    MockHttpServletResponse limited = new MockHttpServletResponse();
    filter.doFilter(request("POST", "/api/registrations", "1.1.1.1"), limited, chain);
    MockHttpServletResponse otherIp = new MockHttpServletResponse();
    filter.doFilter(request("POST", "/api/registrations", "2.2.2.2"), otherIp, chain);
    MockHttpServletResponse read = new MockHttpServletResponse();
    filter.doFilter(request("GET", "/api/options", "1.1.1.1"), read, chain);

    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
    assertThat(limited.getContentType()).startsWith("application/problem+json");
    assertThat(limited.getContentAsString()).contains("\"status\":429");
    assertThat(otherIp.getStatus()).isEqualTo(200);
    assertThat(read.getStatus()).isEqualTo(200);
    assertThat(passed).hasSize(4);

    clock.now = clock.now.plus(Duration.ofSeconds(59));
    MockHttpServletResponse stillLimited = new MockHttpServletResponse();
    filter.doFilter(request("POST", "/api/registrations", "1.1.1.1"), stillLimited, chain);
    assertThat(stillLimited.getHeader("Retry-After")).isEqualTo("1");
    clock.now = clock.now.plus(Duration.ofSeconds(1));
    MockHttpServletResponse nextWindow = new MockHttpServletResponse();
    filter.doFilter(request("POST", "/api/registrations", "1.1.1.1"), nextWindow, chain);
    assertThat(nextWindow.getStatus()).isEqualTo(200);
  }

  @Test
  void rateLimitCountsOrganizerRequestsAndIgnoresNonApiPaths() throws Exception {
    RateLimitFilter filter =
        new RateLimitFilter(new RateLimitFilter.Limits(1, 1, 1), new MutableClock());

    filter.doFilter(
        request("GET", "/api/organizer/export", "1.1.1.1"), new MockHttpServletResponse(), chain);
    MockHttpServletResponse second = new MockHttpServletResponse();
    filter.doFilter(request("GET", "/api/organizer/export", "1.1.1.1"), second, chain);
    for (int i = 0; i < 3; i++) {
      filter.doFilter(
          request("GET", "/actuator/health", "1.1.1.1"), new MockHttpServletResponse(), chain);
    }

    assertThat(second.getStatus()).isEqualTo(429);
    assertThat(passed).hasSize(4);
  }

  @Test
  void requestSizeRejectsDeclaredAndStreamedBodiesAboveTheLimit() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);

    MockHttpServletRequest declared = request("POST", "/api/registrations", "1.1.1.1");
    declared.setContent("12345678901".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse tooLarge = new MockHttpServletResponse();
    filter.doFilter(declared, tooLarge, chain);
    assertThat(tooLarge.getStatus()).isEqualTo(413);

    MockHttpServletRequest chunked = chunked("12345678901");
    MockHttpServletResponse streamedTooLarge = new MockHttpServletResponse();
    filter.doFilter(chunked, streamedTooLarge, chain);
    assertThat(streamedTooLarge.getStatus()).isEqualTo(413);
    assertThat(passed).isEmpty();
  }

  @Test
  void requestSizeAcceptsABodyOfExactlyTheLimit() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);
    MockHttpServletRequest declared = request("POST", "/api/registrations", "1.1.1.1");
    declared.setContent("1234567890".getBytes(StandardCharsets.UTF_8));

    filter.doFilter(declared, new MockHttpServletResponse(), chain);
    filter.doFilter(chunked("1234567890"), new MockHttpServletResponse(), chain);

    assertThat(passed).hasSize(2);
  }

  @Test
  void requestSizePassesSmallBodiesAndReplaysAStreamedBody() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);
    MockHttpServletRequest small = request("POST", "/api/registrations", "1.1.1.1");
    small.setContent("123".getBytes(StandardCharsets.UTF_8));
    filter.doFilter(small, new MockHttpServletResponse(), chain);

    filter.doFilter(chunked("čšž"), new MockHttpServletResponse(), chain);

    assertThat(passed).hasSize(2);
    HttpServletRequest replayed = passed.get(1);
    assertThat(replayed.getContentLengthLong()).isEqualTo(6);
    assertThat(replayed.getContentLength()).isEqualTo(6);
    assertThat(replayed.getReader().readLine()).isEqualTo("čšž");
    assertThat(replayed.getInputStream().isReady()).isTrue();
  }

  private static MockHttpServletRequest chunked(String body) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    return request;
  }

  @Test
  void organizerHttpsFilterRefusesPlainHttpFromOtherHostsOnly() throws Exception {
    OrganizerHttpsFilter filter = new OrganizerHttpsFilter(true);

    MockHttpServletResponse remotePlain = new MockHttpServletResponse();
    filter.doFilter(request("GET", "/api/organizer/export", "203.0.113.5"), remotePlain, chain);
    assertThat(remotePlain.getStatus()).isEqualTo(403);

    MockHttpServletRequest secure = request("GET", "/api/organizer/export", "203.0.113.5");
    secure.setSecure(true);
    filter.doFilter(secure, new MockHttpServletResponse(), chain);
    filter.doFilter(
        request("GET", "/api/organizer/export", "127.0.0.1"), new MockHttpServletResponse(), chain);
    filter.doFilter(
        request("GET", "/api/organizer/export", "::1"), new MockHttpServletResponse(), chain);
    filter.doFilter(
        request("POST", "/api/registrations", "203.0.113.5"), new MockHttpServletResponse(), chain);
    assertThat(passed).hasSize(4);

    MockHttpServletResponse hostName = new MockHttpServletResponse();
    filter.doFilter(request("GET", "/api/organizer/export", "localhost.evil"), hostName, chain);
    assertThat(hostName.getStatus()).isEqualTo(403);
  }

  @Test
  void organizerHttpsFilterCanBeSwitchedOffForLocalUse() throws Exception {
    new OrganizerHttpsFilter(false)
        .doFilter(
            request("GET", "/api/organizer/export", "203.0.113.5"),
            new MockHttpServletResponse(),
            chain);

    assertThat(passed).hasSize(1);
  }

  @Test
  void entryPointAnswers401WithABasicChallenge() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    new ProblemAuthenticationEntryPoint().commence(new MockHttpServletRequest(), response, null);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Basic realm=\"organizer\"");
    assertThat(response.getContentAsString())
        .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401}");
  }
}
