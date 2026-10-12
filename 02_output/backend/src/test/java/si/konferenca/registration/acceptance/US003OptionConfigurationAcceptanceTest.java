package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;

/**
 * US-003: the option configuration file changes what is offered after a restart (AC-003-02), and an
 * invalid file stops the startup (AC-003-03). Each test starts the application itself.
 */
class US003OptionConfigurationAcceptanceTest {

  private static final String CONSENT =
      "\"consent\": {\"id\": \"data-processing\", \"text\": \"I agree.\"}";

  @TempDir Path dir;

  private Path optionsFile(String name, String options) throws Exception {
    Path file = dir.resolve(name);
    Files.writeString(
        file, "{" + CONSENT + ", \"options\": [" + options + "]}", StandardCharsets.UTF_8);
    return file;
  }

  private ConfigurableApplicationContext start(Path options) throws Exception {
    Map<String, Object> p = new HashMap<>();
    p.put("server.port", "0");
    p.put("DB_URL", AcceptanceTestBase.POSTGRES.getJdbcUrl());
    p.put("DB_USERNAME", AcceptanceTestBase.POSTGRES.getUsername());
    p.put("POSTGRES_PASSWORD", AcceptanceTestBase.POSTGRES.getPassword());
    p.put("spring.datasource.url", AcceptanceTestBase.POSTGRES.getJdbcUrl());
    p.put("spring.datasource.username", AcceptanceTestBase.POSTGRES.getUsername());
    p.put("spring.datasource.password", AcceptanceTestBase.POSTGRES.getPassword());
    p.put("MAIL_HOST", AcceptanceTestBase.MAILPIT.getHost());
    p.put("MAIL_PORT", AcceptanceTestBase.MAILPIT.getMappedPort(1025));
    p.put("MAIL_TLS", "false");
    p.put("OPTIONS_LOCATION", "file:" + options.toAbsolutePath());
    p.put("JSON_COPY_DIR", Files.createTempDirectory(dir, "copies").toString());
    p.put("RECAPTCHA_TEST_MODE", "true");
    p.put("ORGANIZER_USERNAME", AcceptanceTestBase.ORGANIZER_USERNAME);
    p.put("ORGANIZER_PASSWORD", AcceptanceTestBase.ORGANIZER_PASSWORD);
    p.put("ORGANIZER_EMAILS", AcceptanceTestBase.ORGANIZER_EMAIL_1);
    return new SpringApplicationBuilder(RegistrationApplication.class).properties(p).run();
  }

  private static Api api(ConfigurableApplicationContext context) {
    return new Api("http://localhost:" + context.getEnvironment().getProperty("local.server.port"));
  }

  @Test
  @DisplayName("AC-003-02 a changed configuration changes the offered options after a restart")
  void ac003_02_changedConfigurationChangesOfferedOptions() throws Exception {
    Path before =
        optionsFile(
            "before.json",
            "{\"id\": \"ws-one\", \"name\": \"Workshop one\", \"category\": \"WORKSHOP\","
                + " \"active\": true},"
                + "{\"id\": \"ev-two\", \"name\": \"Event two\", \"category\": \"EVENT\","
                + " \"active\": true}");
    Path after =
        optionsFile(
            "after.json",
            "{\"id\": \"ws-one\", \"name\": \"Workshop one\", \"category\": \"WORKSHOP\","
                + " \"active\": true},"
                + "{\"id\": \"ev-two\", \"name\": \"Event two\", \"category\": \"EVENT\","
                + " \"active\": false},"
                + "{\"id\": \"meal-new\", \"name\": \"New dinner\", \"category\": \"MEAL\","
                + " \"active\": true}");

    List<String> idsBefore;
    try (ConfigurableApplicationContext context = start(before)) {
      Response response = api(context).get("/api/options");
      assertThat(response.status()).as(response.toString()).isEqualTo(200);
      idsBefore = response.json("$[*].id");
    }
    try (ConfigurableApplicationContext context = start(after)) {
      Api api = api(context);
      Response options = api.get("/api/options");
      assertThat(options.status()).as(options.toString()).isEqualTo(200);
      List<String> idsAfter = options.json("$[*].id");

      assertThat(idsBefore).containsExactly("ws-one", "ev-two");
      assertThat(idsAfter).containsExactly("ws-one", "meal-new");

      Map<String, Object> withNew = Registrations.validExternal();
      withNew.put("optionIds", List.of("meal-new"));
      Response accepted = api.postRegistration(withNew);
      assertThat(accepted.status()).as("participant fields unchanged: %s", accepted).isEqualTo(201);

      Map<String, Object> withDeactivated = Registrations.validExternal();
      withDeactivated.put("optionIds", List.of("ev-two"));
      Response rejected = api.postRegistration(withDeactivated);
      assertThat(rejected.status()).as(rejected.toString()).isEqualTo(400);
      assertThat(rejected.errorFields()).contains("optionIds");
    }
  }

  @Test
  @DisplayName("AC-003-03 an unknown category stops the startup and names the entry")
  void ac003_03_unknownCategoryStopsStartup() throws Exception {
    Path bad =
        optionsFile(
            "bad-category.json",
            "{\"id\": \"ws-fine\", \"name\": \"Fine\", \"category\": \"WORKSHOP\", \"active\":"
                + " true},"
                + "{\"id\": \"party-time\", \"name\": \"Party\", \"category\": \"PARTY\","
                + " \"active\": true}");

    assertStartupFailsNaming(bad, "party-time");
  }

  @Test
  @DisplayName("AC-003-03 a duplicated option id stops the startup and names the entry")
  void ac003_03_duplicatedIdStopsStartup() throws Exception {
    Path bad =
        optionsFile(
            "duplicate.json",
            "{\"id\": \"ws-twice\", \"name\": \"First\", \"category\": \"WORKSHOP\", \"active\":"
                + " true},"
                + "{\"id\": \"ws-twice\", \"name\": \"Second\", \"category\": \"WORKSHOP\","
                + " \"active\": true}");

    assertStartupFailsNaming(bad, "ws-twice");
  }

  private void assertStartupFailsNaming(Path options, String entryId) {
    Throwable failure =
        catchThrowable(
            () -> {
              try (ConfigurableApplicationContext context = start(options)) {
                context.isRunning();
              }
            });

    assertThat(failure).as("startup with an invalid option file").isNotNull();
    List<String> messages = new ArrayList<>();
    for (Throwable t = failure; t != null; t = t.getCause()) {
      messages.add(String.valueOf(t.getMessage()));
    }
    assertThat(String.join("\n", messages)).contains(entryId);
  }
}
