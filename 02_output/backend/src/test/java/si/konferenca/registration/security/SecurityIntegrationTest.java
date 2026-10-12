package si.konferenca.registration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.TestStack;

/** Integration of the security chain: headers, CORS, denied paths, health, password storage. */
@SpringBootTest(classes = RegistrationApplication.class)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

  private static final String ORIGIN = "http://localhost:5173";

  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(registry, Map.of("APP_CORS_ALLOWED_ORIGIN", ORIGIN));
  }

  @Test
  void publicResponsesCarrySecurityHeaders() throws Exception {
    MvcResult result = mvc.perform(get("/api/form-config")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getHeader("Content-Security-Policy"))
        .isEqualTo("default-src 'none'; frame-ancestors 'none'");
    assertThat(result.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
    assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(result.getResponse().getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
  }

  @Test
  void corsAllowsOnlyTheConfiguredOrigin() throws Exception {
    MvcResult allowed =
        mvc.perform(
                options("/api/registrations")
                    .header("Origin", ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type"))
            .andReturn();
    MvcResult other =
        mvc.perform(
                options("/api/registrations")
                    .header("Origin", "https://evil.example")
                    .header("Access-Control-Request-Method", "POST"))
            .andReturn();

    assertThat(allowed.getResponse().getStatus()).isEqualTo(200);
    assertThat(allowed.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
    assertThat(allowed.getResponse().getHeader("Access-Control-Allow-Methods")).contains("POST");
    assertThat(other.getResponse().getStatus()).isEqualTo(403);
    assertThat(other.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
  }

  @Test
  void everythingElseIsDeniedWithAGenericBody() throws Exception {
    MvcResult result = mvc.perform(get("/api/registrations")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(401);
    assertThat(result.getResponse().getHeader("WWW-Authenticate")).startsWith("Basic realm=");
    assertThat(result.getResponse().getContentAsString())
        .isEqualTo("{\"message\":\"Organizer credentials are required.\"}");
  }

  @Test
  void healthAndReadinessArePublic() throws Exception {
    assertThat(mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus())
        .isEqualTo(200);
    MvcResult readiness = mvc.perform(get("/actuator/health/readiness")).andReturn();
    assertThat(readiness.getResponse().getStatus()).isEqualTo(200);
    assertThat(readiness.getResponse().getContentAsString()).contains("UP");
    assertThat(mvc.perform(get("/actuator/env")).andReturn().getResponse().getStatus())
        .isNotEqualTo(200);
  }

  @Test
  void theOrganizerPasswordIsKeptOnlyAsBcryptHash() {
    String stored = users.loadUserByUsername(TestStack.ORGANIZER_USERNAME).getPassword();

    assertThat(stored).startsWith("$2").doesNotContain(TestStack.ORGANIZER_PASSWORD);
  }
}
