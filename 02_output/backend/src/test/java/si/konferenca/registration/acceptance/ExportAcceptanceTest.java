package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.DefaultContextAcceptanceTest;
import si.konferenca.registration.acceptance.support.Payloads;
import si.konferenca.registration.acceptance.support.TestInfrastructure;
import tools.jackson.databind.JsonNode;

/** US-008 Registration export (AC-008-01 … AC-008-04). */
class ExportAcceptanceTest extends DefaultContextAcceptanceTest {

  private static final String EXPORT = "/api/organizer/registrations.xlsx";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private static Map<String, String> organizer() {
    return Map.of(
        "Authorization",
        basicAuth(TestInfrastructure.ORGANIZER_USERNAME, TestInfrastructure.ORGANIZER_PASSWORD));
  }

  private static List<List<String>> rows(byte[] workbook) throws Exception {
    DataFormatter formatter = new DataFormatter();
    List<List<String>> rows = new ArrayList<>();
    try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(workbook))) {
      Sheet sheet = book.getSheetAt(0);
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (Cell cell : row) {
          cells.add(formatter.formatCellValue(cell));
        }
        rows.add(cells);
      }
    }
    return rows;
  }

  private static Optional<List<String>> rowWith(List<List<String>> rows, String value) {
    return rows.stream().filter(r -> r.contains(value)).findFirst();
  }

  private static boolean hasTime(List<String> row, Instant expected) {
    return row.stream()
        .anyMatch(
            cell -> {
              try {
                return Instant.parse(cell).equals(expected);
              } catch (DateTimeParseException e) {
                return false;
              }
            });
  }

  private static void assertNoRegistrationData(HttpResponse<byte[]> response, String email) {
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .doesNotContain("spreadsheetml");
    assertThat(new String(response.body(), StandardCharsets.UTF_8)).doesNotContain(email);
    assertThat(new String(response.body(), StandardCharsets.ISO_8859_1)).doesNotStartWith("PK");
  }

  @Test
  void AC_008_01_organizerDownloadsWorkbookWithOneRowPerRegistration() throws Exception {
    String externalEmail = Payloads.uniqueEmail("ac00801e");
    String studentEmail = Payloads.uniqueEmail("ac00801s");
    HttpResponse<String> external = postRegistration(Payloads.external(externalEmail));
    assertThat(external.statusCode()).as(external.body()).isEqualTo(201);
    registerOk(Payloads.student(studentEmail));
    JsonNode externalCreated = json(external.body());

    HttpResponse<byte[]> response = getBytes(EXPORT, organizer());
    int registrations = registrationCount();

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith(XLSX);
    assertThat(response.headers().firstValue("Content-Disposition").orElse(""))
        .contains("attachment", ".xlsx");
    List<List<String>> rows = rows(response.body());
    assertThat(rows).hasSize(registrations + 1);

    List<String> externalRow = rowWith(rows, externalEmail).orElseThrow();
    assertThat(externalRow).contains("EXTERNAL", "Ana", "Novak", "Acme Testing Ltd");
    assertThat(String.join(" | ", externalRow))
        .contains("Secure coding workshop", "Opening reception", "privacy");
    assertThat(hasTime(externalRow, Instant.parse(externalCreated.path("registeredAt").asString())))
        .as("registration time in %s", externalRow)
        .isTrue();

    List<String> studentRow = rowWith(rows, studentEmail).orElseThrow();
    assertThat(studentRow)
        .contains(
            "STUDENT", "Bor", "Kranjc", "University of Maribor", "Computer Science", "63210001");
    assertThat(String.join(" | ", studentRow))
        .contains("Test automation workshop", "Lunch", "privacy");
  }

  @Test
  void AC_008_02_exportWithoutCredentialsIsRefusedWithoutData() {
    String email = Payloads.uniqueEmail("ac00802a");
    registerOk(Payloads.external(email));

    HttpResponse<byte[]> response = getBytes(EXPORT, Map.of());

    assertThat(response.statusCode()).isEqualTo(401);
    assertNoRegistrationData(response, email);
  }

  @Test
  void AC_008_02_exportWithWrongCredentialsIsRefusedWithoutData() {
    String email = Payloads.uniqueEmail("ac00802b");
    registerOk(Payloads.external(email));

    HttpResponse<byte[]> wrongPassword =
        getBytes(
            EXPORT,
            Map.of(
                "Authorization",
                basicAuth(TestInfrastructure.ORGANIZER_USERNAME, "wrong-password")));
    HttpResponse<byte[]> wrongUser =
        getBytes(
            EXPORT,
            Map.of("Authorization", basicAuth("someone", TestInfrastructure.ORGANIZER_PASSWORD)));

    assertThat(wrongPassword.statusCode()).isEqualTo(401);
    assertNoRegistrationData(wrongPassword, email);
    assertThat(wrongUser.statusCode()).isEqualTo(401);
    assertNoRegistrationData(wrongUser, email);
  }

  @Test
  void AC_008_03_slovenianCharactersAreUnchangedInWorkbookCells() throws Exception {
    String email = Payloads.uniqueEmail("ac00803");
    Map<String, Object> body = Payloads.external(email);
    body.put("firstName", "Čedomir Žiga");
    body.put("lastName", "Šuštaršič");
    body.put("organization", "Univerza v Ljubljani, Fakulteta za računalništvo (ČŠŽ čšž)");
    registerOk(body);

    HttpResponse<byte[]> response = getBytes(EXPORT, organizer());

    assertThat(response.statusCode()).isEqualTo(200);
    List<String> row = rowWith(rows(response.body()), email).orElseThrow();
    assertThat(row)
        .contains(
            "Čedomir Žiga",
            "Šuštaršič",
            "Univerza v Ljubljani, Fakulteta za računalništvo (ČŠŽ čšž)");
  }

  @Test
  void AC_008_04_validCredentialsOverPlainHttpFromRemoteClientAreRefused() {
    String email = Payloads.uniqueEmail("ac00804");
    registerOk(Payloads.external(email));
    Map<String, String> remotePlainHttp = new java.util.HashMap<>(organizer());
    remotePlainHttp.put("X-Forwarded-For", "203.0.113.10");

    HttpResponse<byte[]> response = getBytes(EXPORT, remotePlainHttp);

    assertThat(response.statusCode()).isNotEqualTo(200).isEqualTo(403);
    assertNoRegistrationData(response, email);
  }

  @Test
  void AC_008_04_validCredentialsOverHttpsFromRemoteClientAreAccepted() {
    Map<String, String> remoteHttps = new java.util.HashMap<>(organizer());
    remoteHttps.put("X-Forwarded-For", "203.0.113.10");
    remoteHttps.put("X-Forwarded-Proto", "https");

    HttpResponse<byte[]> response = getBytes(EXPORT, remoteHttps);

    assertThat(response.statusCode()).isEqualTo(200);
  }
}
