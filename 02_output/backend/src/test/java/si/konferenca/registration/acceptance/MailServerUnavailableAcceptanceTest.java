package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;
import tools.jackson.databind.json.JsonMapper;

/** US-006: with the SMTP server unreachable a valid registration is still accepted (OQ-03). */
@SpringBootTest
@AutoConfigureMockMvc
class MailServerUnavailableAcceptanceTest {

  private static final Path JSON_COPY_DIR = AcceptanceEnvironment.newTempDir("json-copies-nomail");

  @Autowired MockMvc mvc;

  private final JdbcTemplate jdbc = AcceptanceEnvironment.jdbc();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(
        registry,
        Map.of(
            "spring.mail.host", "127.0.0.1",
            "spring.mail.port", String.valueOf(closedPort()),
            "app.json-copy.dir", JSON_COPY_DIR.toString()));
  }

  @BeforeEach
  void cleanState() {
    AcceptanceTestBase.clearStorage(jdbc);
    AcceptanceTestBase.clearDirectory(JSON_COPY_DIR);
  }

  @Test
  void AC_006_03_registrationIsAcceptedAndStoredWhenMailServerIsUnavailable() throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/registrations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .content(
                        JsonMapper.builder()
                            .build()
                            .writeValueAsBytes(Submissions.validExternal())))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    String id =
        JsonMapper.builder()
            .build()
            .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
            .path("id")
            .asString();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM registration WHERE id = ?",
                Integer.class,
                UUID.fromString(id)))
        .isEqualTo(1);
    assertThat(JSON_COPY_DIR.resolve(id + ".json")).isRegularFile();
  }

  /** A local port with nothing listening on it. */
  private static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
