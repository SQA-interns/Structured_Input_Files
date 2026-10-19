package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestSizeLimitFilterTest {

  final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(100);

  @Test
  void SR_03_declaredLargeBodyIsRefusedWith413() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[101]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isNull();
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).contains("\"fieldErrors\":[]");
  }

  @Test
  void SR_03_bodyWithinTheLimitPasses() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[100]);
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isNotNull();
    assertThat(chain.getRequest().getInputStream().readAllBytes()).hasSize(100);
  }

  @Test
  void SR_03_undeclaredLargeBodyFailsWhileReading() throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }

          @Override
          public int getContentLength() {
            return -1;
          }
        };
    request.setContent(new byte[500]);
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    ServletRequest passed = chain.getRequest();
    assertThat(passed).isNotNull();
    assertThatThrownBy(() -> passed.getInputStream().readAllBytes())
        .isInstanceOf(IOException.class);
  }
}
