package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  /** A clock the test moves forward. */
  static final class MutableClock extends Clock {
    Instant now = Instant.parse("2026-10-19T12:00:10Z");

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

  final MutableClock clock = new MutableClock();
  final RateLimitFilter filter = new RateLimitFilter(clock, 3, 2, 1);

  MockHttpServletResponse call(String method, String path, String address) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(address);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    if (chain.getRequest() == null && response.getStatus() == 200) {
      response.setStatus(-1);
    }
    return response;
  }

  @Test
  void SR_03_requestsAboveTheLimitGet429WithRetryAfter() throws Exception {
    assertThat(call("POST", "/api/registrations", "10.0.0.1").getStatus()).isEqualTo(200);
    assertThat(call("POST", "/api/registrations", "10.0.0.1").getStatus()).isEqualTo(200);

    MockHttpServletResponse refused = call("POST", "/api/registrations", "10.0.0.1");

    assertThat(refused.getStatus()).isEqualTo(429);
    assertThat(Integer.parseInt(refused.getHeader("Retry-After"))).isBetween(1, 60);
    assertThat(refused.getContentAsString()).contains("\"fieldErrors\":[]");
  }

  @Test
  void SR_03_eachClientHasItsOwnLimit() throws Exception {
    call("POST", "/api/registrations", "10.0.0.1");
    call("POST", "/api/registrations", "10.0.0.1");

    assertThat(call("POST", "/api/registrations", "10.0.0.2").getStatus()).isEqualTo(200);
  }

  @Test
  void SR_03_eachEndpointHasItsOwnLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      assertThat(call("GET", "/api/form-config", "10.0.0.1").getStatus()).isEqualTo(200);
    }
    assertThat(call("GET", "/api/form-config", "10.0.0.1").getStatus()).isEqualTo(429);
    assertThat(call("POST", "/api/registrations", "10.0.0.1").getStatus()).isEqualTo(200);
    assertThat(call("GET", "/api/registrations/export", "10.0.0.1").getStatus()).isEqualTo(200);
    assertThat(call("GET", "/api/registrations/export", "10.0.0.1").getStatus()).isEqualTo(429);
  }

  @Test
  void SR_03_theLimitResetsInTheNextMinute() throws Exception {
    call("GET", "/api/registrations/export", "10.0.0.1");
    assertThat(call("GET", "/api/registrations/export", "10.0.0.1").getStatus()).isEqualTo(429);

    clock.now = clock.now.plus(Duration.ofSeconds(60));

    assertThat(call("GET", "/api/registrations/export", "10.0.0.1").getStatus()).isEqualTo(200);
  }

  @Test
  void SR_03_otherApiPathsShareTheStrictestLimitAndNonApiPathsAreNotLimited() throws Exception {
    assertThat(call("GET", "/api/unknown", "10.0.0.1").getStatus()).isEqualTo(200);
    assertThat(call("GET", "/api/unknown", "10.0.0.1").getStatus()).isEqualTo(429);
    for (int i = 0; i < 5; i++) {
      assertThat(call("GET", "/actuator/health/liveness", "10.0.0.1").getStatus()).isEqualTo(200);
    }
  }
}
