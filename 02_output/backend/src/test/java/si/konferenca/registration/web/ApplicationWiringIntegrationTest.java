package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;

/**
 * Covers AC-003-01, AC-008-02: the wiring of security, filters and settings in a context built for
 * this class alone (phase 5 survivors), so that a changed bean definition is always used.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class ApplicationWiringIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwordEncoder;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(
        registry,
        Map.of(
            "app.recaptcha.site-key",
            "public-site-key",
            "app.rate-limit.export-per-minute",
            "2",
            "app.max-request-bytes",
            "1024",
            "app.json-copy.dir",
            AcceptanceEnvironment.newTempDir("json-copies-wiring").toString()));
  }

  @Test
  void AC_003_01_testModeHidesTheSiteKeyInTheFormConfiguration() throws Exception {
    String body =
        mvc.perform(get("/api/form-config"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(body).contains("\"siteKey\":\"\"", "\"testMode\":true", "Konferenca Test 2026");
  }

  @Test
  void AC_008_02_theChallengeNamesTheOrganizerRealm() throws Exception {
    MvcResult result = mvc.perform(get("/api/registrations/export")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(401);
    assertThat(result.getResponse().getHeader("WWW-Authenticate"))
        .startsWith("Basic realm=\"Organizer export\"");
  }

  @Test
  void AC_008_02_failedLoginsCountTowardsTheExportLimit() throws Exception {
    String wrong =
        "Basic "
            + Base64.getEncoder()
                .encodeToString("organizer:wrong".getBytes(StandardCharsets.UTF_8));
    int[] statuses = new int[3];
    for (int i = 0; i < 3; i++) {
      statuses[i] =
          mvc.perform(
                  get("/api/registrations/export")
                      .header("Authorization", wrong)
                      .secure(true)
                      .with(
                          r -> {
                            r.setRemoteAddr("192.0.2.44");
                            return r;
                          }))
              .andReturn()
              .getResponse()
              .getStatus();
    }

    assertThat(statuses).containsExactly(401, 401, 429);
  }

  @Test
  void SR_03_oversizedBodiesAreRefusedBeforeSecurity() throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/not-a-resource")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"x\":\"" + "y".repeat(2000) + "\"}")
                    .with(
                        r -> {
                          r.setRemoteAddr("192.0.2.45");
                          return r;
                        }))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(413);
  }

  @Test
  void AC_008_02_passwordsAreEncodedWithBcrypt() {
    assertThat(passwordEncoder.encode("secret")).startsWith("{bcrypt}$2");
  }
}
