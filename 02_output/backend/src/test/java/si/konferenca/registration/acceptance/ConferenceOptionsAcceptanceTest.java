package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.AcceptanceSupport;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestEnvironment;
import tools.jackson.databind.JsonNode;

/** US-003 Configurable conference options. */
@ExtendWith(OutputCaptureExtension.class)
class ConferenceOptionsAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_003_01_onlyActiveOptionsAreReturnedWithTheirAttributesAndConsents() {
    HttpResponse<String> response = get("/api/form-config");

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode config = json(response);
    assertThat(config.path("conferenceName").asString()).isEqualTo(TestEnvironment.CONFERENCE_NAME);
    assertThat(config.path("options").valueStream().map(o -> o.path("id").asString()))
        .containsExactlyInAnyOrder(
            "ws-ai", "ev-gala", "ev-student-night", "meal-lunch", "other-tour");
    JsonNode workshop = option(config, "ws-ai");
    assertThat(workshop.path("name").asString()).isEqualTo("Delavnica: umetna inteligenca");
    assertThat(workshop.path("category").asString()).isEqualTo("WORKSHOP");
    assertThat(workshop.path("audience").valueStream().map(JsonNode::asString))
        .containsExactlyInAnyOrder("EXTERNAL", "STUDENT");
    assertThat(option(config, "ev-gala").path("category").asString()).isEqualTo("EVENT");
    assertThat(option(config, "meal-lunch").path("category").asString()).isEqualTo("MEAL");
    assertThat(option(config, "other-tour").path("category").asString()).isEqualTo("OTHER");
    JsonNode dataProcessing = consent(config, "data-processing");
    assertThat(dataProcessing.path("mandatory").asBoolean()).isTrue();
    assertThat(dataProcessing.path("text").asString())
        .isEqualTo("Strinjam se z obdelavo osebnih podatkov za organizacijo konference.");
    assertThat(consent(config, "photos").path("mandatory").asBoolean()).isFalse();
    assertThat(config.path("recaptcha").path("siteKey").asString())
        .isEqualTo(TestEnvironment.SITE_KEY);
    assertThat(config.path("recaptcha").path("testMode").asBoolean()).isFalse();
  }

  @Test
  void ac_003_02_changedConfigurationFileChangesOfferedOptionsAfterRestart() throws IOException {
    Path file = Files.createTempFile("conference-options", ".json");
    write(
        file,
        """
        {"options": [
          {"id": "ws-first", "name": "Prva delavnica", "category": "WORKSHOP", "active": true},
          {"id": "ev-second", "name": "Drugi dogodek", "category": "EVENT", "active": true}
        ],
         "consents": [{"id": "data-processing", "text": "Soglasje.", "mandatory": true}]}
        """);

    assertThat(offeredOptionIds(file)).containsExactlyInAnyOrder("ws-first", "ev-second");

    write(
        file,
        """
        {"options": [
          {"id": "ws-first", "name": "Prva delavnica", "category": "WORKSHOP", "active": true},
          {"id": "ev-second", "name": "Drugi dogodek", "category": "EVENT", "active": false},
          {"id": "other-new", "name": "Nova dejavnost", "category": "OTHER", "active": true}
        ],
         "consents": [{"id": "data-processing", "text": "Soglasje.", "mandatory": true}]}
        """);

    try (Started app = start(file)) {
      assertThat(optionIds(app.fetch("/api/form-config")))
          .containsExactlyInAnyOrder("ws-first", "other-new");
      Map<String, Object> body = validExternal(uniqueEmail("ac-003-02"));
      body.put("optionIds", List.of("other-new"));
      body.put("consentIds", List.of("data-processing"));
      HttpResponse<String> accepted = app.submit(body);
      assertThat(accepted.statusCode()).as(accepted.body()).isEqualTo(201);
    }
  }

  static Stream<Arguments> invalidConfigurations() {
    String consent =
        "\"consents\": [{\"id\": \"data-processing\", \"text\": \"S.\", \"mandatory\": true}]";
    return Stream.of(
        Arguments.of(
            "duplicate id",
            "{\"options\": [{\"id\": \"ws-dup\", \"name\": \"A\", \"category\": \"WORKSHOP\", \"active\": true},"
                + " {\"id\": \"ws-dup\", \"name\": \"B\", \"category\": \"WORKSHOP\", \"active\": true}], "
                + consent
                + "}",
            "ws-dup"),
        Arguments.of(
            "unknown category",
            "{\"options\": [{\"id\": \"bad-category\", \"name\": \"A\", \"category\": \"PARTY\", \"active\": true}], "
                + consent
                + "}",
            "bad-category"),
        Arguments.of(
            "unknown audience",
            "{\"options\": [{\"id\": \"bad-audience\", \"name\": \"A\", \"category\": \"MEAL\", \"active\": true,"
                + " \"audience\": [\"TEACHER\"]}], "
                + consent
                + "}",
            "bad-audience"),
        Arguments.of(
            "missing name",
            "{\"options\": [{\"id\": \"no-name\", \"category\": \"EVENT\", \"active\": true}], "
                + consent
                + "}",
            "no-name"));
  }

  @ParameterizedTest(name = "AC-003-03 {0}")
  @MethodSource("invalidConfigurations")
  void ac_003_03_invalidConfigurationStopsTheBackend(
      String problem, String content, String offendingId, CapturedOutput output)
      throws IOException {
    Path file = Files.createTempFile("conference-options-invalid", ".json");
    write(file, content);

    assertThatThrownBy(() -> start(file).close()).isInstanceOf(Exception.class);
    assertThat(output.getAll()).contains(offendingId);
  }

  // ---------------------------------------------------------------- helpers

  private static JsonNode option(JsonNode config, String id) {
    return config
        .path("options")
        .valueStream()
        .filter(o -> id.equals(o.path("id").asString()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("option " + id + " missing"));
  }

  private static JsonNode consent(JsonNode config, String id) {
    return config
        .path("consents")
        .valueStream()
        .filter(c -> id.equals(c.path("id").asString()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("consent " + id + " missing"));
  }

  private static List<String> optionIds(HttpResponse<String> response) {
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    return json(response).path("options").valueStream().map(o -> o.path("id").asString()).toList();
  }

  private static List<String> offeredOptionIds(Path file) {
    try (Started app = start(file)) {
      return optionIds(app.fetch("/api/form-config"));
    }
  }

  private static void write(Path file, String content) throws IOException {
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }

  /** Starts the backend as a separate application with the given options file. */
  private static Started start(Path optionsFile) {
    Map<String, Supplier<Object>> overrides =
        Map.of(
            "app.options.location", () -> "file:" + optionsFile.toAbsolutePath(),
            "server.port", () -> "0",
            "spring.jmx.enabled", () -> "false");
    ConfigurableApplicationContext context =
        new SpringApplicationBuilder(RegistrationApplication.class)
            .run(TestEnvironment.asArguments(overrides));
    return new Started(context);
  }

  /** A started backend; closing it stops the application. */
  private static final class Started extends AcceptanceSupport implements AutoCloseable {
    private final ConfigurableApplicationContext context;

    Started(ConfigurableApplicationContext context) {
      this.context = context;
    }

    @Override
    protected int port() {
      return Integer.parseInt(context.getEnvironment().getProperty("local.server.port", "0"));
    }

    HttpResponse<String> fetch(String path) {
      return get(path);
    }

    HttpResponse<String> submit(Map<String, Object> body) {
      return register(body);
    }

    @Override
    public void close() {
      context.close();
    }
  }
}
