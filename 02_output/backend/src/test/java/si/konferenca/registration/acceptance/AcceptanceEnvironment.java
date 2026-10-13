package si.konferenca.registration.acceptance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared test environment of the backend acceptance tests (spec §9): PostgreSQL and Mailpit
 * containers, the JSON copy directory, and read-only helpers for the database, the mail catcher and
 * the copy directory. Only public interfaces and stored outcomes are inspected.
 */
public final class AcceptanceEnvironment {

  public static final JsonMapper JSON = JsonMapper.builder().build();

  public static final String CONFERENCE_NAME = "Acceptance Conference 2026";
  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "acceptance-organizer-secret-42";
  public static final List<String> ORGANIZER_EMAILS =
      List.of("org1@konferenca.test", "org2@konferenca.test");
  public static final String PASS_TOKEN = "test-pass";

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("registration")
          .withUsername("registration")
          .withPassword("acceptance-db-password");

  public static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.31.1")
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/api/v1/messages").forPort(8025));

  public static final Path JSON_COPY_DIR;

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  static {
    POSTGRES.start();
    MAILPIT.start();
    try {
      JSON_COPY_DIR = Files.createTempDirectory("acceptance-json-copies");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private AcceptanceEnvironment() {}

  /** Settings that differ between acceptance test classes. */
  public record Settings(
      String conferenceConfig, String jsonCopyDir, String mailHost, int mailPort) {

    /** The default settings: shipped test configuration, working copy directory and Mailpit. */
    public static Settings defaults() {
      return new Settings(
          "classpath:acceptance/conference.json",
          JSON_COPY_DIR.toString(),
          MAILPIT.getHost(),
          MAILPIT.getMappedPort(1025));
    }

    public Settings withConferenceConfig(String location) {
      return new Settings(location, jsonCopyDir, mailHost, mailPort);
    }

    public Settings withJsonCopyDir(String dir) {
      return new Settings(conferenceConfig, dir, mailHost, mailPort);
    }

    public Settings withMailServer(String host, int port) {
      return new Settings(conferenceConfig, jsonCopyDir, host, port);
    }
  }

  /** Registers every backend property (spec §3) for the given settings. */
  public static void register(DynamicPropertyRegistry registry, Settings settings) {
    properties(settings).forEach((key, value) -> registry.add(key, () -> value));
  }

  /** The backend properties (spec §3) for the given settings, as plain strings. */
  public static Map<String, String> properties(Settings settings) {
    Map<String, String> p = new LinkedHashMap<>();
    p.put("spring.datasource.url", POSTGRES.getJdbcUrl());
    p.put("spring.datasource.username", POSTGRES.getUsername());
    p.put("spring.datasource.password", POSTGRES.getPassword());
    p.put("spring.mail.host", settings.mailHost());
    p.put("spring.mail.port", String.valueOf(settings.mailPort()));
    p.put("spring.mail.properties.mail.smtp.starttls.enable", "false");
    p.put("spring.mail.properties.mail.smtp.starttls.required", "false");
    p.put("app.mail.from", "registration@konferenca.test");
    p.put("app.mail.organizer-recipients", String.join(",", ORGANIZER_EMAILS));
    p.put("app.conference.name", CONFERENCE_NAME);
    p.put("app.conference.config-location", settings.conferenceConfig());
    p.put("app.storage.json-copy-dir", settings.jsonCopyDir());
    p.put("app.recaptcha.test-mode", "true");
    p.put("app.organizer.username", ORGANIZER_USERNAME);
    p.put("app.organizer.password", ORGANIZER_PASSWORD);
    p.put("app.organizer.https-only", "true");
    p.put("app.rate-limit.form-config-per-minute", "100000");
    p.put("app.rate-limit.registrations-per-minute", "100000");
    p.put("app.rate-limit.export-per-minute", "100000");
    return p;
  }

  /** Removes every stored registration, JSON copy and caught email. */
  public static void reset() {
    execute(
        "DO $$ BEGIN IF to_regclass('public.registration') IS NOT NULL THEN"
            + " TRUNCATE registration CASCADE; END IF; END $$;");
    clearJsonCopies();
    mailpit("DELETE", "/api/v1/messages");
  }

  // ---------------------------------------------------------------- database

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static void execute(String sql) {
    try (Connection c = connection();
        Statement s = c.createStatement()) {
      s.execute(sql);
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Runs a query and returns every row as column label → value (JDBC default types). */
  public static List<Map<String, Object>> query(String sql, Object... args) {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      for (int i = 0; i < args.length; i++) {
        ps.setObject(i + 1, args[i]);
      }
      try (ResultSet rs = ps.executeQuery()) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int columns = rs.getMetaData().getColumnCount();
        while (rs.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= columns; i++) {
            row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
          }
          rows.add(row);
        }
        return rows;
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Number of stored registrations; 0 when the table does not exist yet. */
  public static long registrationCount() {
    List<Map<String, Object>> exists =
        query("SELECT to_regclass('public.registration') IS NOT NULL AS present");
    if (!Boolean.TRUE.equals(exists.get(0).get("present"))) {
      return 0;
    }
    return ((Number) query("SELECT count(*) AS n FROM registration").get(0).get("n")).longValue();
  }

  // ---------------------------------------------------------------- JSON copies

  public static List<Path> jsonCopies() {
    try (Stream<Path> files = Files.list(JSON_COPY_DIR)) {
      return files.filter(f -> f.getFileName().toString().endsWith(".json")).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path jsonCopy(String registrationId) {
    return JSON_COPY_DIR.resolve(registrationId + ".json");
  }

  private static void clearJsonCopies() {
    try (Stream<Path> files = Files.list(JSON_COPY_DIR)) {
      for (Path f : files.toList()) {
        Files.deleteIfExists(f);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  // ---------------------------------------------------------------- Mailpit

  private static HttpResponse<byte[]> mailpit(String method, String path) {
    URI uri = URI.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + path);
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();
    try {
      return HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static JsonNode mailpitJson(String path) {
    return JSON.readTree(new String(mailpit("GET", path).body(), StandardCharsets.UTF_8));
  }

  /** Summaries of every caught message (Mailpit list API). */
  public static List<JsonNode> messages() {
    List<JsonNode> result = new ArrayList<>();
    mailpitJson("/api/v1/messages?limit=1000").get("messages").forEach(result::add);
    return result;
  }

  /** Summaries of the caught messages whose To contains the address. */
  public static List<JsonNode> messagesTo(String address) {
    return messages().stream()
        .filter(
            m -> {
              for (JsonNode to : m.get("To")) {
                if (address.equalsIgnoreCase(to.get("Address").asString())) {
                  return true;
                }
              }
              return false;
            })
        .toList();
  }

  /**
   * Waits up to 15 s until at least {@code count} messages to the address were caught, then returns
   * them (possibly fewer if the time ran out).
   */
  public static List<JsonNode> awaitMessagesTo(String address, int count) {
    long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    List<JsonNode> found = messagesTo(address);
    while (found.size() < count && System.nanoTime() < deadline) {
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
      found = messagesTo(address);
    }
    return found;
  }

  /** Full message (Text, HTML, To, Cc, Bcc, Attachments) by Mailpit id. */
  public static JsonNode message(String id) {
    return mailpitJson("/api/v1/message/" + URLEncoder.encode(id, StandardCharsets.UTF_8));
  }

  /** Raw headers of a message: header name → list of values. */
  public static JsonNode headers(String id) {
    return mailpitJson(
        "/api/v1/message/" + URLEncoder.encode(id, StandardCharsets.UTF_8) + "/headers");
  }

  /** Bytes of one MIME part (attachment) of a message. */
  public static byte[] part(String id, String partId) {
    return mailpit(
            "GET",
            "/api/v1/message/"
                + URLEncoder.encode(id, StandardCharsets.UTF_8)
                + "/part/"
                + URLEncoder.encode(partId, StandardCharsets.UTF_8))
        .body();
  }
}
