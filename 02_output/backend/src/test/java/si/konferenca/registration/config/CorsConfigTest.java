package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

  @Test
  void configuredOriginIsAllowedOnApiOnly() {
    var source = SecurityConfig.cors(" http://127.0.0.1:5173 ");

    CorsConfiguration api =
        source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/form-config"));
    assertThat(api).isNotNull();
    assertThat(api.getAllowedOrigins()).containsExactly("http://127.0.0.1:5173");
    assertThat(api.getAllowedMethods()).containsExactly("GET", "POST");
    assertThat(api.getAllowedHeaders()).containsExactly("Content-Type", "Authorization");
    assertThat(source.getCorsConfiguration(new MockHttpServletRequest("GET", "/actuator/health")))
        .isNull();
  }

  @Test
  void noOriginMeansNoCors() {
    assertThat(
            SecurityConfig.cors("")
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/form-config")))
        .isNull();
    assertThat(
            SecurityConfig.cors(null)
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/form-config")))
        .isNull();
  }
}
