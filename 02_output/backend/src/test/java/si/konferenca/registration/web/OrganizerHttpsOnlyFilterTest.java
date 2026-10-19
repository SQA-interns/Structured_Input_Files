package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OrganizerHttpsOnlyFilterTest {

  static MockHttpServletRequest export(String remoteAddr, boolean secure) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/registrations/export");
    request.setRemoteAddr(remoteAddr);
    request.setSecure(secure);
    return request;
  }

  static boolean passes(OrganizerHttpsOnlyFilter filter, MockHttpServletRequest request)
      throws Exception {
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    return chain.getRequest() != null;
  }

  @Test
  void AC_008_04_plainHttpFromARemoteClientIsRefusedWithoutData() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    new OrganizerHttpsOnlyFilter(true).doFilter(export("203.0.113.10", false), response, chain);

    assertThat(chain.getRequest()).isNull();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).startsWith("application/json");
    assertThat(response.getContentAsString()).contains("\"fieldErrors\":[]");
  }

  @Test
  void AC_008_04_httpsFromARemoteClientPasses() throws Exception {
    assertThat(passes(new OrganizerHttpsOnlyFilter(true), export("203.0.113.10", true))).isTrue();
  }

  @ParameterizedTest(name = "AC-008-04 loopback {0}")
  @ValueSource(strings = {"127.0.0.1", "127.0.0.2", "::1", "0:0:0:0:0:0:0:1"})
  void AC_008_04_plainHttpFromLoopbackPasses(String address) throws Exception {
    assertThat(passes(new OrganizerHttpsOnlyFilter(true), export(address, false))).isTrue();
  }

  @ParameterizedTest(name = "AC-008-04 not loopback {0}")
  @ValueSource(strings = {"10.0.0.5", "localhost.evil.example", "", "not an address"})
  void AC_008_04_otherAddressesOverPlainHttpAreRefused(String address) throws Exception {
    assertThat(passes(new OrganizerHttpsOnlyFilter(true), export(address, false))).isFalse();
  }

  @Test
  void AC_008_04_switchedOffItLetsPlainHttpThrough() throws Exception {
    assertThat(passes(new OrganizerHttpsOnlyFilter(false), export("203.0.113.10", false))).isTrue();
  }

  @Test
  void AC_008_04_publicEndpointsAreNotAffected() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/form-config");
    request.setRemoteAddr("203.0.113.10");

    assertThat(passes(new OrganizerHttpsOnlyFilter(true), request)).isTrue();
  }
}
