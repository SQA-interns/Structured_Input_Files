package si.konferenca.registration.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Tests added for mutants that survived the phase 5 PIT run (docs/03_test-strategy.md). */
class SurvivingMutantsApiTest {

  @Test
  void limiterEvictsExactlyTheWindowsThatExpired() {
    RequestFiltersTest.MutableClock clock = new RequestFiltersTest.MutableClock();
    FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock);
    for (int i = 0; i <= 10_000; i++) {
      limiter.acquire("k" + i, 1);
    }
    clock.now = clock.now.plusSeconds(30);
    limiter.acquire("young", 1);
    assertThat(limiter.trackedKeys()).isEqualTo(10_002);

    clock.now = clock.now.plusSeconds(30);
    limiter.acquire("new", 1);

    assertThat(limiter.trackedKeys()).isEqualTo(2);
  }

  @Test
  void handlersAnswerWithTheErrorShape() throws Exception {
    ApiExceptionHandler h = new ApiExceptionHandler();

    assertThat(h.mediaType(new HttpMediaTypeNotSupportedException("x")).getStatusCode())
        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(h.method(new HttpRequestMethodNotSupportedException("PUT")).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(
            h.notFound(
                    new NoResourceFoundException(
                        org.springframework.http.HttpMethod.GET, "/x", "x"))
                .getBody()
                .message())
        .isEqualTo("Not found.");
    var unexpected = h.unexpected(new IllegalStateException("secret detail"));
    assertThat(unexpected.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(unexpected.getBody().message()).doesNotContain("secret");
    assertThat(ErrorResponse.of("m")).isEqualTo(new ErrorResponse("m", java.util.List.of()));
  }

  @Test
  void rootCauseTypeFollowsTheCauseChain() {
    Exception root = new java.io.IOException("disk");
    assertThat(
            ApiExceptionHandler.rootCauseType(
                new RuntimeException(new IllegalStateException(root))))
        .isEqualTo("IOException");
    assertThat(ApiExceptionHandler.rootCauseType(new IllegalStateException("alone")))
        .isEqualTo("IllegalStateException");
  }

  @Test
  void cachedBodyStreamReportsReadinessAndEnd() throws Exception {
    RequestLimitFilter filter =
        new RequestLimitFilter(
            new FixedWindowRateLimiter(Clock.systemUTC()),
            new RequestLimitFilter.Limits(10, 10, 10, 100));
    MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/registrations");
    post.setContent("ab".getBytes(StandardCharsets.UTF_8));
    AtomicReference<String> states = new AtomicReference<>();
    filter.doFilter(
        post,
        new MockHttpServletResponse(),
        new MockFilterChain() {
          @Override
          public void doFilter(ServletRequest req, ServletResponse res) throws java.io.IOException {
            ServletInputStream in = ((HttpServletRequest) req).getInputStream();
            String before = in.isReady() + "/" + in.isFinished();
            in.readAllBytes();
            states.set(before + "->" + in.isFinished());
          }
        });

    assertThat(states.get()).isEqualTo("true/false->true");
  }
}
