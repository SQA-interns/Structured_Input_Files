package si.konferenca.registration.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Black-box acceptance tests of the backend: requests through the HTTP API (MockMvc runs the full
 * filter chain), outcomes read from the storage tables, the JSON copy directory and Mailpit.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AcceptanceTestBase {

  protected static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired protected MockMvc mvc;

  protected final JdbcTemplate jdbc = AcceptanceEnvironment.jdbc();
  protected final MailpitClient mailpit = AcceptanceEnvironment.mailpit();

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(registry, Map.of());
  }

  @BeforeEach
  void cleanState() {
    clearStorage(jdbc);
    clearDirectory(jsonCopyDir());
    mailpit.deleteAll();
  }

  protected Path jsonCopyDir() {
    return AcceptanceEnvironment.defaultJsonCopyDir();
  }

  protected MvcResult submit(Map<String, Object> body) throws Exception {
    return mvc.perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8)
                .content(JSON.writeValueAsBytes(body)))
        .andReturn();
  }

  protected static JsonNode json(MvcResult result) throws Exception {
    return JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  /** Field names of the fieldErrors of an ErrorResponse. */
  protected static List<String> errorFields(MvcResult result) throws Exception {
    List<String> fields = new ArrayList<>();
    for (JsonNode error : json(result).path("fieldErrors")) {
      fields.add(error.path("field").asString());
    }
    return fields;
  }

  protected int registrationCount() {
    return tableExists(jdbc, "registration")
        ? jdbc.queryForObject("SELECT COUNT(*) FROM registration", Integer.class)
        : 0;
  }

  protected List<Path> jsonCopies() {
    try (Stream<Path> files = Files.list(jsonCopyDir())) {
      return files.toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** "Rejected" of acceptance-criteria.md: no row, no JSON copy, no email to the participant. */
  protected void assertNothingStoredOrSent(String participantEmail) {
    assertThat(registrationCount()).as("registration rows").isZero();
    assertThat(jsonCopies()).as("JSON copies").isEmpty();
    assertThat(mailpit.messagesToAfterGracePeriod(participantEmail)).as("emails").isEmpty();
  }

  public static boolean tableExists(JdbcTemplate jdbc, String table) {
    return jdbc.queryForObject(
        "SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + table);
  }

  public static void clearStorage(JdbcTemplate jdbc) {
    for (String table : List.of("registration_consent", "registration_option", "registration")) {
      if (tableExists(jdbc, table)) {
        jdbc.execute("DELETE FROM " + table);
      }
    }
  }

  public static void clearDirectory(Path dir) {
    try {
      if (Files.isRegularFile(dir)) {
        Files.delete(dir);
      }
      Files.createDirectories(dir);
      try (Stream<Path> files = Files.list(dir)) {
        for (Path file : files.toList()) {
          Files.delete(file);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
