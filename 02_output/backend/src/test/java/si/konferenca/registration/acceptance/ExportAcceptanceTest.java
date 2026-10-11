package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;
import static si.konferenca.registration.acceptance.support.Payloads.student;
import static si.konferenca.registration.acceptance.support.Payloads.with;
import static si.konferenca.registration.acceptance.support.TestEnvironment.ORGANIZER_PASSWORD;
import static si.konferenca.registration.acceptance.support.TestEnvironment.ORGANIZER_USERNAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Api;

/** US-008 Registration export. */
class ExportAcceptanceTest extends AcceptanceTestBase {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Test
  @DisplayName("AC-008-01: an organizer downloads a workbook with one row per registration")
  void ac_008_01_organizerExportsWorkbook() throws IOException {
    Map<String, Object> ext = external();
    Map<String, Object> stu = student();
    UUID extId = registerAccepted(ext);
    UUID stuId = registerAccepted(stu);

    Api.Response response =
        api.getAsOrganizer("/api/export", ORGANIZER_USERNAME, ORGANIZER_PASSWORD);

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.header("Content-Type")).startsWith(XLSX);
    assertThat(response.header("Content-Disposition")).contains("attachment").contains(".xlsx");
    Map<String, Map<String, String>> rows = rowsById(response.body());

    Map<String, String> e = rows.get(extId.toString());
    assertThat(e).as("row of the external registration").isNotNull();
    assertThat(e.get("Type")).isEqualTo("External participant");
    assertThat(e.get("First name")).isEqualTo(ext.get("firstName"));
    assertThat(e.get("Last name")).isEqualTo(ext.get("lastName"));
    assertThat(e.get("Email")).isEqualTo(ext.get("email"));
    assertThat(e.get("Organization / institution")).isEqualTo(ext.get("organization"));
    assertThat(e.get("Workshops")).contains("Workshop: AI v praksi");
    assertThat(e.get("Meals")).contains("Kosilo");
    assertThat(e.get("Consents")).contains("privacy");
    assertThat(Instant.parse(e.get("Registered at"))).isNotNull();

    Map<String, String> s = rows.get(stuId.toString());
    assertThat(s).as("row of the student registration").isNotNull();
    assertThat(s.get("Type")).isEqualTo("Student");
    assertThat(s.get("Study institution")).isEqualTo(stu.get("studyInstitution"));
    assertThat(s.get("Study programme")).isEqualTo(stu.get("studyProgramme"));
    assertThat(s.get("Student ID")).isEqualTo(stu.get("studentId"));
    assertThat(s.get("Organization / institution")).isEmpty();
    assertThat(s.get("Events")).contains("Gala večerja");
  }

  @Test
  @DisplayName("AC-008-02: the export is refused without or with wrong credentials")
  void ac_008_02_exportRefusedWithoutValidCredentials() {
    Map<String, Object> ext = external();
    registerAccepted(ext);
    String lastName = (String) ext.get("lastName");

    Api.Response none = api.get("/api/export");
    Api.Response wrongPassword =
        api.getAsOrganizer("/api/export", ORGANIZER_USERNAME, ORGANIZER_PASSWORD + "x");
    Api.Response wrongUser = api.getAsOrganizer("/api/export", "intruder", ORGANIZER_PASSWORD);

    for (Api.Response r : new Api.Response[] {none, wrongPassword, wrongUser}) {
      assertThat(r.status()).isEqualTo(401);
      assertThat(r.header("Content-Type"))
          .satisfiesAnyOf(
              ct -> assertThat(ct).isNull(), ct -> assertThat(ct).doesNotStartWith(XLSX));
      assertThat(r.text()).doesNotContain(lastName);
    }
  }

  @Test
  @DisplayName("AC-008-03: Slovenian characters are unchanged in the workbook")
  void ac_008_03_slovenianCharactersSurviveExport() throws IOException {
    Map<String, Object> ext = external();
    String lastName = "Žnidaršič-Čuk " + ext.get("lastName");
    ext = with(ext, "firstName", "Špela");
    ext = with(ext, "lastName", lastName);
    ext = with(ext, "organization", "Univerza v Mariboru – Fakulteta za elektrotehniko, čšž");
    UUID id = registerAccepted(ext);

    Api.Response response =
        api.getAsOrganizer("/api/export", ORGANIZER_USERNAME, ORGANIZER_PASSWORD);

    assertThat(response.status()).isEqualTo(200);
    Map<String, String> row = rowsById(response.body()).get(id.toString());
    assertThat(row).isNotNull();
    assertThat(row.get("First name")).isEqualTo("Špela");
    assertThat(row.get("Last name")).isEqualTo(lastName);
    assertThat(row.get("Organization / institution"))
        .isEqualTo("Univerza v Mariboru – Fakulteta za elektrotehniko, čšž");
  }

  @Test
  @DisplayName("AC-008-04: valid credentials over plain HTTP from a remote client are refused")
  void ac_008_04_credentialsOverPlainHttpFromRemoteClientAreRefused() {
    registerAccepted(external());

    // The request arrives through a trusted proxy on loopback that reports the remote client.
    Api.Response plainHttp =
        api.getAsOrganizer(
            "/api/export",
            ORGANIZER_USERNAME,
            ORGANIZER_PASSWORD,
            "X-Forwarded-For",
            "203.0.113.7",
            "X-Forwarded-Proto",
            "http");
    Api.Response https =
        api.getAsOrganizer(
            "/api/export",
            ORGANIZER_USERNAME,
            ORGANIZER_PASSWORD,
            "X-Forwarded-For",
            "203.0.113.7",
            "X-Forwarded-Proto",
            "https");

    assertThat(plainHttp.status()).isEqualTo(403);
    assertThat(plainHttp.header("Content-Type"))
        .satisfiesAnyOf(ct -> assertThat(ct).isNull(), ct -> assertThat(ct).doesNotStartWith(XLSX));
    // Control: the same remote client over HTTPS is served.
    assertThat(https.status()).isEqualTo(200);
    assertThat(https.header("Content-Type")).startsWith(XLSX);
  }

  /** Reads sheet "Registrations": header row names → cell text, keyed by "Registration ID". */
  private static Map<String, Map<String, String>> rowsById(byte[] xlsx) throws IOException {
    DataFormatter formatter = new DataFormatter();
    Map<String, Map<String, String>> result = new HashMap<>();
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = workbook.getSheet("Registrations");
      assertThat(sheet).as("sheet Registrations").isNotNull();
      Row header = sheet.getRow(0);
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        Map<String, String> values = new HashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
          Cell cell = row.getCell(c);
          values.put(
              formatter.formatCellValue(header.getCell(c)),
              cell == null ? "" : formatter.formatCellValue(cell));
        }
        result.put(values.get("Registration ID"), values);
      }
    }
    return result;
  }
}
