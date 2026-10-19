package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;

/** BR-08, SB-02, SB-03, SR-06: organizer access to the export. */
@SpringBootTest
@AutoConfigureMockMvc
class ExportSecurityIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired UserDetailsService users;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(registry, Map.of());
  }

  static String basic(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  static MockHttpServletRequestBuilder organizer() {
    return get("/api/registrations/export")
        .header(
            "Authorization",
            basic(
                AcceptanceEnvironment.ORGANIZER_USERNAME,
                AcceptanceEnvironment.ORGANIZER_PASSWORD));
  }

  @Test
  void AC_008_02_missingCredentialsAre401WithBasicChallenge() throws Exception {
    MvcResult result = mvc.perform(get("/api/registrations/export")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(401);
    assertThat(result.getResponse().getHeader("WWW-Authenticate")).startsWith("Basic");
  }

  @Test
  void AC_008_02_wrongPasswordIs401() throws Exception {
    MvcResult result =
        mvc.perform(
                get("/api/registrations/export")
                    .header(
                        "Authorization", basic(AcceptanceEnvironment.ORGANIZER_USERNAME, "nope")))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void AC_008_01_validCredentialsFromLoopbackGetTheWorkbook() throws Exception {
    MvcResult result = mvc.perform(organizer()).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentType()).startsWith("application/vnd.openxml");
  }

  @Test
  void AC_008_04_validCredentialsOverHttpsFromRemoteClientGetTheWorkbook() throws Exception {
    MvcResult result =
        mvc.perform(
                organizer()
                    .secure(true)
                    .with(
                        r -> {
                          r.setRemoteAddr("203.0.113.10");
                          return r;
                        }))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
  }

  @Test
  void AC_008_04_validCredentialsOverPlainHttpFromRemoteClientAre403() throws Exception {
    MvcResult result =
        mvc.perform(
                organizer()
                    .with(
                        r -> {
                          r.setRemoteAddr("203.0.113.10");
                          return r;
                        }))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(403);
  }

  @Test
  void AC_008_02_passwordIsKeptOnlyAsBcryptHash() {
    String stored =
        users.loadUserByUsername(AcceptanceEnvironment.ORGANIZER_USERNAME).getPassword();

    assertThat(stored).doesNotContain(AcceptanceEnvironment.ORGANIZER_PASSWORD);
    assertThat(stored).matches("(\\{bcrypt\\})?\\$2[aby]?\\$\\d\\d\\$.{53}");
  }
}
