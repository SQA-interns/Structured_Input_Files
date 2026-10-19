package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Covers AC-008-04: responses and edge cases of the web filters (phase 5 survivors, SR-03). */
class FilterResponsesTest {

  static MockHttpServletResponse call(jakarta.servlet.Filter filter, MockHttpServletRequest request)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  static MockHttpServletRequest request(String method, String path, String address) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(address);
    return request;
  }

  @Test
  void SR_03_refusalsAreJsonInUtf8() throws Exception {
    RateLimitFilter rate = new RateLimitFilter(new RateLimitFilterTest.MutableClock(), 1, 1, 1);
    call(rate, request("GET", "/api/form-config", "10.0.0.9"));
    MockHttpServletResponse limited = call(rate, request("GET", "/api/form-config", "10.0.0.9"));
    assertThat(limited.getContentType()).startsWith("application/json");
    assertThat(limited.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");

    MockHttpServletRequest big = request("POST", "/api/registrations", "10.0.0.9");
    big.setContent(new byte[20]);
    MockHttpServletResponse tooLarge = call(new RequestSizeLimitFilter(10), big);
    assertThat(tooLarge.getContentType()).startsWith("application/json");
    assertThat(tooLarge.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");

    MockHttpServletResponse https =
        call(
            new OrganizerHttpsOnlyFilter(true),
            request("GET", "/api/registrations/export", "10.0.0.9"));
    assertThat(https.getContentType()).startsWith("application/json");
    assertThat(https.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
  }

  @Test
  void SR_03_requestsInTheSameMinuteShareOneWindow() throws Exception {
    RateLimitFilterTest.MutableClock clock = new RateLimitFilterTest.MutableClock();
    RateLimitFilter rate = new RateLimitFilter(clock, 1, 1, 1);

    assertThat(call(rate, request("GET", "/api/form-config", "10.0.0.1")).getStatus())
        .isEqualTo(200);
    clock.now = clock.now.plus(Duration.ofSeconds(5));

    assertThat(call(rate, request("GET", "/api/form-config", "10.0.0.1")).getStatus())
        .isEqualTo(429);
  }

  @SuppressWarnings("unchecked")
  static Map<String, ?> windows(RateLimitFilter filter) throws Exception {
    Field field = RateLimitFilter.class.getDeclaredField("windows");
    field.setAccessible(true);
    return (Map<String, ?>) field.get(filter);
  }

  @Test
  void SR_03_oldWindowsAreKeptWhileFewClientsAreTracked() throws Exception {
    RateLimitFilterTest.MutableClock clock = new RateLimitFilterTest.MutableClock();
    RateLimitFilter rate = new RateLimitFilter(clock, 5, 5, 5);
    call(rate, request("GET", "/api/form-config", "10.0.0.1"));
    clock.now = clock.now.plus(Duration.ofSeconds(60));

    call(rate, request("GET", "/api/form-config", "10.0.0.2"));

    assertThat(windows(rate)).hasSize(2);
  }

  @Test
  void SR_03_cleanupStartsAboveTenThousandWindowsAndRemovesOnlyOldOnes() throws Exception {
    RateLimitFilterTest.MutableClock clock = new RateLimitFilterTest.MutableClock();
    RateLimitFilter rate = new RateLimitFilter(clock, 5, 2, 5);
    for (int i = 0; i < 10_000; i++) {
      call(rate, request("GET", "/api/form-config", "old-" + i));
    }
    clock.now = clock.now.plus(Duration.ofSeconds(60));
    call(rate, request("POST", "/api/registrations", "10.0.0.1"));
    assertThat(windows(rate)).as("exactly 10000 old windows are not cleaned").hasSize(10_001);

    call(rate, request("POST", "/api/registrations", "10.0.0.1"));

    assertThat(windows(rate)).as("above 10000 the old minute is removed").hasSize(1);
    assertThat(call(rate, request("POST", "/api/registrations", "10.0.0.1")).getStatus())
        .as("the current window survives the cleanup")
        .isEqualTo(429);
  }

  @ParameterizedTest(name = "AC-008-04 refused [{0}]")
  @ValueSource(strings = {"2001:db8::1", "fe80::1", ":::::", "::g"})
  void AC_008_04_nonLoopbackOrInvalidIpv6IsRefused(String address) throws Exception {
    assertThat(
            OrganizerHttpsOnlyFilterTest.passes(
                new OrganizerHttpsOnlyFilter(true),
                OrganizerHttpsOnlyFilterTest.export(address, false)))
        .isFalse();
  }

  @Test
  void SR_03_theLimitedBodyStreamDelegatesItsState() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[] {1, 2});
    MockFilterChain chain = new MockFilterChain();
    new RequestSizeLimitFilter(100).doFilter(request, new MockHttpServletResponse(), chain);
    ServletInputStream body = chain.getRequest().getInputStream();

    assertThat(body.isFinished()).isFalse();
    assertThat(body.isReady()).isTrue();
    body.readAllBytes();
    assertThat(body.isFinished()).isTrue();
    assertThatThrownBy(
            () ->
                body.setReadListener(
                    new ReadListener() {
                      @Override
                      public void onDataAvailable() {}

                      @Override
                      public void onAllDataRead() {}

                      @Override
                      public void onError(Throwable t) {}
                    }))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
