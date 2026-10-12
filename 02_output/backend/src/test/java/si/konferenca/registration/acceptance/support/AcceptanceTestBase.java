package si.konferenca.registration.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import si.konferenca.registration.RegistrationApplication;
import tools.jackson.databind.json.JsonMapper;

/**
 * Black-box acceptance tests through the REST API (docs/02_contracts/openapi.json). Concrete
 * classes declare their settings with a {@code @DynamicPropertySource} that calls {@link
 * TestStack#register}.
 */
@SpringBootTest(classes = RegistrationApplication.class)
@AutoConfigureMockMvc
public abstract class AcceptanceTestBase {

  protected static final String PRIVACY_TEXT =
      "I agree that my personal data is processed for organizing the conference and that it is"
          + " shared with the conference organizers.";

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired protected MockMvc mvc;

  @Autowired protected JdbcTemplate jdbc;

  protected final Mailpit mailpit = TestStack.mailpit();

  /** Every test starts with no registration, no JSON copy and no email. */
  @BeforeEach
  void cleanState() {
    Boolean tableExists =
        jdbc.queryForObject("SELECT to_regclass('public.registration') IS NOT NULL", Boolean.class);
    if (Boolean.TRUE.equals(tableExists)) {
      jdbc.execute("TRUNCATE registration CASCADE");
    }
    deleteFiles(jsonCopyDir());
    mailpit.deleteAll();
  }

  /** The JSON copy directory of this context. */
  protected Path jsonCopyDir() {
    return TestStack.JSON_COPY_DIR;
  }

  // ---- request builders -------------------------------------------------------------------

  /** A valid external submission (acceptance-criteria.md "valid external submission"). */
  protected static Map<String, Object> validExternal() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", "ana.novak@example.org");
    body.put("organization", "Institut Example");
    body.put("optionIds", new ArrayList<>(List.of("ws-testing", "meal-lunch")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("recaptchaToken", TestStack.TEST_MODE_TOKEN);
    return body;
  }

  /** A valid student submission (acceptance-criteria.md "valid student submission"). */
  protected static Map<String, Object> validStudent() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Kranjc");
    body.put("email", "luka.kranjc@student.example.org");
    body.put("studyInstitution", "Univerza Example");
    body.put("studyProgramme", "Computer Science");
    body.put("studentId", "63200001");
    body.put("optionIds", new ArrayList<>(List.of("ws-testing", "ev-opening")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("recaptchaToken", TestStack.TEST_MODE_TOKEN);
    return body;
  }

  protected static String json(Object value) {
    return JSON.writeValueAsString(value);
  }

  protected MvcResult submit(Map<String, Object> body) throws Exception {
    return mvc.perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8)
                .content(json(body)))
        .andReturn();
  }

  /** Submits and returns the new registration id; fails unless the answer is 201. */
  protected String register(Map<String, Object> body) throws Exception {
    MvcResult result = submit(body);
    assertThat(result.getResponse().getStatus())
        .as("status of %s", result.getResponse().getContentAsString(StandardCharsets.UTF_8))
        .isEqualTo(201);
    return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
  }

  protected static MockHttpServletRequestBuilder exportRequest() {
    return get("/api/export");
  }

  protected static String basic(String username, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  protected static MockHttpServletRequestBuilder withOrganizerCredentials(
      MockHttpServletRequestBuilder request) {
    return request.header(
        HttpHeaders.AUTHORIZATION,
        basic(TestStack.ORGANIZER_USERNAME, TestStack.ORGANIZER_PASSWORD));
  }

  // ---- response helpers -------------------------------------------------------------------

  protected static String body(MvcResult result) throws IOException {
    return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  /** Field names listed in the {@code fieldErrors} of an error response. */
  protected static List<String> errorFields(MvcResult result) throws IOException {
    return JsonPath.read(body(result), "$.fieldErrors[*].field");
  }

  /** Asserts the "rejected" outcome: 400, an error for the field, nothing stored, nothing sent. */
  protected void assertRejectedWithFieldError(MvcResult result, String field) throws Exception {
    assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(400);
    assertThat(errorFields(result)).as(body(result)).contains(field);
    assertNothingChanged();
  }

  /** No database row, no JSON copy and no email (acceptance-criteria.md "Rejected"). */
  protected void assertNothingChanged() throws Exception {
    assertThat(registrationCount()).as("registrations in the database").isZero();
    assertThat(jsonCopies()).as("JSON copies").isEmpty();
    Thread.sleep(300);
    assertThat(mailpit.total()).as("emails sent").isZero();
  }

  // ---- storage helpers --------------------------------------------------------------------

  protected int registrationCount() {
    Boolean tableExists =
        jdbc.queryForObject("SELECT to_regclass('public.registration') IS NOT NULL", Boolean.class);
    if (!Boolean.TRUE.equals(tableExists)) {
      return 0;
    }
    Integer count = jdbc.queryForObject("SELECT count(*) FROM registration", Integer.class);
    return count == null ? 0 : count;
  }

  protected List<Path> jsonCopies() throws IOException {
    Path dir = jsonCopyDir();
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(dir)) {
      return files.filter(Files::isRegularFile).toList();
    }
  }

  protected Path jsonCopy(String id) {
    return jsonCopyDir().resolve(id + ".json");
  }

  protected String readJsonCopy(String id) throws IOException {
    return Files.readString(jsonCopy(id), StandardCharsets.UTF_8);
  }

  private static void deleteFiles(Path dir) {
    if (!Files.isDirectory(dir)) {
      return;
    }
    try (Stream<Path> files = Files.list(dir)) {
      for (Path file : files.toList()) {
        if (Files.isRegularFile(file)) {
          Files.delete(file);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
