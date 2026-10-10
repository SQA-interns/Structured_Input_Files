package si.konferenca.registration.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** HTTP, storage and email helpers shared by the acceptance tests (black box). */
public abstract class AcceptanceSupport {

  protected static final JsonMapper JSON = JsonMapper.builder().build();
  protected static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  /** PostgreSQL state when the schema does not exist yet: no registration is stored. */
  private static final String UNDEFINED_TABLE = "42P01";

  private final HttpClient http = HttpClient.newHttpClient();

  /** Port of the application under test. */
  protected abstract int port();

  // ---------------------------------------------------------------- HTTP

  protected String baseUrl() {
    return "http://127.0.0.1:" + port();
  }

  /** A client address of its own for each call, so that per-client limits do not interfere. */
  protected static String randomClientIp() {
    ThreadLocalRandom r = ThreadLocalRandom.current();
    return "198.18." + r.nextInt(0, 256) + "." + r.nextInt(1, 255);
  }

  protected HttpResponse<String> get(String path) {
    return send(
        HttpRequest.newBuilder(URI.create(baseUrl() + path))
            .header("X-Forwarded-For", randomClientIp())
            .GET()
            .build());
  }

  protected HttpResponse<String> postJson(String path, String json, String clientIp) {
    return send(
        HttpRequest.newBuilder(URI.create(baseUrl() + path))
            .header("Content-Type", "application/json")
            .header("X-Forwarded-For", clientIp)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build());
  }

  protected HttpResponse<String> register(Map<String, Object> body) {
    return register(body, randomClientIp());
  }

  protected HttpResponse<String> register(Map<String, Object> body, String clientIp) {
    return postJson("/api/registrations", JSON.writeValueAsString(body), clientIp);
  }

  /** Registers and asserts acceptance; returns the response body. */
  protected JsonNode registerAccepted(Map<String, Object> body) {
    HttpResponse<String> response = register(body);
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    return JSON.readTree(response.body());
  }

  /** Export request; {@code headers} are added as given (credentials, forwarded headers). */
  protected HttpResponse<byte[]> export(Map<String, String> headers) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(baseUrl() + "/api/export/registrations.xlsx")).GET();
    headers.forEach(builder::header);
    try {
      return http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  protected static Map<String, String> organizerHeaders(String clientIp) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(
        "Authorization",
        basic(TestEnvironment.ORGANIZER_USERNAME, TestEnvironment.ORGANIZER_PASSWORD));
    headers.put("X-Forwarded-For", clientIp);
    headers.put("X-Forwarded-Proto", "https");
    return headers;
  }

  protected static String basic(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  /** Downloads the export as the organizer and returns all rows (header first) as text. */
  protected List<List<String>> exportRows() {
    HttpResponse<byte[]> response = export(organizerHeaders(randomClientIp()));
    assertThat(response.statusCode()).isEqualTo(200);
    return readWorkbook(response.body());
  }

  protected static List<List<String>> readWorkbook(byte[] xlsx) {
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      DataFormatter formatter = new DataFormatter();
      Sheet sheet = workbook.getSheetAt(0);
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
          Cell cell = row.getCell(c);
          cells.add(cell == null ? "" : formatter.formatCellValue(cell));
        }
        rows.add(cells);
      }
      return rows;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The export row whose cells contain the reference, as column name to value. */
  protected static Map<String, String> exportRow(List<List<String>> rows, String reference) {
    List<String> header = rows.get(0);
    for (List<String> row : rows.subList(1, rows.size())) {
      if (row.contains(reference)) {
        Map<String, String> byColumn = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
          byColumn.put(header.get(i), i < row.size() ? row.get(i) : "");
        }
        return byColumn;
      }
    }
    throw new AssertionError("no export row for " + reference);
  }

  private HttpResponse<String> send(HttpRequest request) {
    try {
      return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  // ---------------------------------------------------------------- request bodies

  protected static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.si";
  }

  protected static Map<String, Object> validExternal(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", email);
    body.put("organization", "Institut Jožef Stefan");
    body.put("optionIds", List.of("ws-ai", "meal-lunch"));
    body.put("consentIds", List.of("data-processing"));
    body.put("captchaToken", CaptchaMock.VALID_TOKEN);
    return body;
  }

  protected static Map<String, Object> validStudent(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Kranjc");
    body.put("email", email);
    body.put("studyInstitution", "Univerza v Mariboru");
    body.put("studyProgramme", "Računalništvo in informatika");
    body.put("studentId", "E1234567");
    body.put("optionIds", List.of("ev-student-night", "meal-lunch"));
    body.put("consentIds", List.of("data-processing"));
    body.put("captchaToken", CaptchaMock.VALID_TOKEN);
    return body;
  }

  // ---------------------------------------------------------------- responses

  protected static JsonNode json(HttpResponse<String> response) {
    return JSON.readTree(response.body());
  }

  /** Asserts the error body carries a field error with the field and code. */
  protected static void assertFieldError(
      HttpResponse<String> response, int status, String field, String code) {
    assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
    JsonNode body = json(response);
    List<String> errors = new ArrayList<>();
    for (JsonNode error : body.path("fieldErrors")) {
      errors.add(error.path("field").asString() + ":" + error.path("code").asString());
    }
    assertThat(errors).as(response.body()).contains(field + ":" + code);
  }

  /** Asserts an error body exposes no internal details (SB-07). */
  protected static void assertNoInternals(String body) {
    assertThat(body)
        .doesNotContain("Exception")
        .doesNotContain("java.")
        .doesNotContain("org.springframework")
        .doesNotContain("si.konferenca")
        .doesNotContain("SQL")
        .doesNotContain("trace");
  }

  // ---------------------------------------------------------------- storage

  protected static Connection database() throws SQLException {
    return DriverManager.getConnection(
        TestEnvironment.POSTGRES.getJdbcUrl(),
        TestEnvironment.POSTGRES.getUsername(),
        TestEnvironment.POSTGRES.getPassword());
  }

  protected static long registrationCount() {
    return queryLong("select count(*) from registration", null);
  }

  protected static long registrationCountByEmail(String email) {
    return queryLong("select count(*) from registration where lower(email) = lower(?)", email);
  }

  /** The registration row with the given reference as column name to text value. */
  protected static Map<String, String> registrationRow(String reference) {
    try (Connection c = database();
        PreparedStatement s =
            c.prepareStatement("select * from registration where reference = ?::uuid")) {
      s.setString(1, reference);
      try (ResultSet rs = s.executeQuery()) {
        if (!rs.next()) {
          return Map.of();
        }
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
          row.put(rs.getMetaData().getColumnName(i), rs.getString(i));
        }
        return row;
      }
    } catch (SQLException e) {
      if (UNDEFINED_TABLE.equals(e.getSQLState())) {
        return Map.of();
      }
      throw new IllegalStateException(e);
    }
  }

  private static long queryLong(String sql, String parameter) {
    try (Connection c = database();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (parameter != null) {
        s.setString(1, parameter);
      }
      try (ResultSet rs = s.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      if (UNDEFINED_TABLE.equals(e.getSQLState())) {
        return 0;
      }
      throw new IllegalStateException(e);
    }
  }

  protected static Path jsonCopy(Path directory, String reference) {
    return directory.resolve("registration-" + reference + ".json");
  }

  protected static long jsonCopyCount(Path directory) {
    if (!Files.isDirectory(directory)) {
      return 0;
    }
    try (Stream<Path> files = Files.list(directory)) {
      return files.filter(p -> p.getFileName().toString().endsWith(".json")).count();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected static String readString(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
