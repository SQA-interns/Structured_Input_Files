package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;

/** SR-03, SB-10, NFR-04 through the full filter chain. */
@SpringBootTest
@AutoConfigureMockMvc
class WebProtectionIntegrationTest {

  @Autowired MockMvc mvc;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(
        registry,
        Map.of(
            "app.rate-limit.registration-per-minute",
            "2",
            "app.max-request-bytes",
            "2048",
            "app.json-copy.dir",
            AcceptanceEnvironment.newTempDir("json-copies-web").toString()));
  }

  @Test
  void SB_10_apiResponsesCarrySecurityHeaders() throws Exception {
    MvcResult result = mvc.perform(get("/api/form-config")).andReturn();

    assertThat(result.getResponse().getHeader("Content-Security-Policy"))
        .isEqualTo("default-src 'none'; frame-ancestors 'none'");
    assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(result.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
    assertThat(result.getResponse().getHeader("Referrer-Policy")).isEqualTo("no-referrer");
  }

  @Test
  void NFR_04_livenessAndReadinessAreUp() throws Exception {
    for (String probe : new String[] {"liveness", "readiness"}) {
      MvcResult result = mvc.perform(get("/actuator/health/" + probe)).andReturn();

      assertThat(result.getResponse().getStatus()).as(probe).isEqualTo(200);
      assertThat(result.getResponse().getContentAsString()).contains("\"status\":\"UP\"");
    }
  }

  @Test
  void NFR_04_otherActuatorEndpointsAreNotExposed() throws Exception {
    for (String path : new String[] {"/actuator/env", "/actuator/beans", "/actuator/health"}) {
      int status = mvc.perform(get(path)).andReturn().getResponse().getStatus();
      if (!"/actuator/health".equals(path)) {
        assertThat(status).as(path).isIn(401, 403, 404);
      }
    }
    String health =
        mvc.perform(get("/actuator/health")).andReturn().getResponse().getContentAsString();
    assertThat(health).doesNotContain("postgres", "jdbc", "details");
  }

  @Test
  void SR_03_registrationsAboveTheLimitGet429() throws Exception {
    int[] statuses = new int[3];
    for (int i = 0; i < 3; i++) {
      statuses[i] =
          mvc.perform(
                  post("/api/registrations")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{}")
                      .with(
                          r -> {
                            r.setRemoteAddr("198.51.100.7");
                            return r;
                          }))
              .andReturn()
              .getResponse()
              .getStatus();
    }

    assertThat(statuses).containsExactly(400, 400, 429);
  }

  @Test
  void SR_03_oversizedBodyGets413() throws Exception {
    String big = "{\"firstName\": \"" + "x".repeat(3000) + "\"}";

    int status =
        mvc.perform(
                post("/api/registrations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(big)
                    .with(
                        r -> {
                          r.setRemoteAddr("198.51.100.8");
                          return r;
                        }))
            .andReturn()
            .getResponse()
            .getStatus();

    assertThat(status).isEqualTo(413);
  }
}
