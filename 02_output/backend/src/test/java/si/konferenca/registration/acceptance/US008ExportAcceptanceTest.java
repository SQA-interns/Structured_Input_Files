package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-008 Excel export for organizers ({@code GET /api/organizer/export}). */
class US008ExportAcceptanceTest extends AcceptanceTestBase {

  static final String EXPORT = "/api/organizer/export";
  static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  static final List<String> COLUMNS =
      List.of(
          "Registration ID",
          "Type",
          "Submitted at",
          "First name",
          "Last name",
          "Email",
          "Organization / institution",
          "Study institution",
          "Study programme",
          "Student ID",
          "Workshops",
          "Events",
          "Meals",
          "Other activities",
          "Consent",
          "Consent given at");

  private String registered(Map<String, Object> request) throws Exception {
    Response response = api.postRegistration(request);
    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    return response.json("$.id");
  }

  private Response export() throws Exception {
    return api.get(EXPORT, ORGANIZER_USERNAME, ORGANIZER_PASSWORD);
  }

  private static List<List<String>> rows(Response response) throws Exception {
    List<List<String>> rows = new ArrayList<>();
    DataFormatter formatter = new DataFormatter();
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.bytes()))) {
      Sheet sheet = workbook.getSheet("Registrations");
      assertThat(sheet).as("sheet Registrations").isNotNull();
      for (Row row : sheet) {
        List<String> values = new ArrayList<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
          Cell cell = row.getCell(c);
          values.add(cell == null ? "" : formatter.formatCellValue(cell));
        }
        rows.add(values);
      }
    }
    return rows;
  }

  @Test
  @DisplayName("AC-008-01 the organizer exports one row per registration with every column")
  void ac008_01_organizerExportsEveryRegistration() throws Exception {
    Map<String, Object> external = Registrations.validExternal();
    Map<String, Object> student = Registrations.validStudent();
    String externalId = registered(external);
    String studentId = registered(student);

    Response response = export();

    assertThat(response.status()).as(response.toString()).isEqualTo(200);
    assertThat(response.header("Content-Type")).startsWith(XLSX);
    assertThat(response.header("Content-Disposition"))
        .contains("attachment")
        .contains("registrations.xlsx");
    List<List<String>> rows = rows(response);
    assertThat(rows).hasSize(3);
    assertThat(rows.get(0)).containsExactlyElementsOf(COLUMNS);
    List<String> ext = rows.get(1);
    assertThat(ext.get(0)).isEqualTo(externalId);
    assertThat(ext.get(1)).isEqualTo("EXTERNAL");
    assertThat(ext.get(2)).isNotBlank();
    assertThat(ext.subList(3, 7))
        .containsExactly("Ana", "Novak", (String) external.get("email"), "Acme d.o.o.");
    assertThat(ext.get(10)).isEqualTo("Workshop A: Testing");
    assertThat(ext.get(12)).isEqualTo("Lunch");
    assertThat(ext.get(14)).isNotBlank();
    assertThat(ext.get(15)).isNotBlank();
    List<String> stu = rows.get(2);
    assertThat(stu.get(0)).isEqualTo(studentId);
    assertThat(stu.get(1)).isEqualTo("STUDENT");
    assertThat(stu.subList(7, 10))
        .containsExactly("University of Ljubljana", "Computer Science", "63210001");
    assertThat(stu.get(10)).isEqualTo("Workshop B: Security");
    assertThat(stu.get(13)).isEqualTo("Career fair");
  }

  @Test
  @DisplayName("AC-008-02 Slovenian characters are unchanged in the export")
  void ac008_02_slovenianCharactersAreUnchanged() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("firstName", "Čedomir");
    request.put("lastName", "Šuštaršič");
    request.put("organization", "Žale d.o.o.");
    assertThat(api.postRegistration(request).status()).isEqualTo(201);

    Response response = export();

    assertThat(response.status()).as(response.toString()).isEqualTo(200);
    List<String> row = rows(response).get(1);
    assertThat(row.subList(3, 5)).containsExactly("Čedomir", "Šuštaršič");
    assertThat(row.get(6)).isEqualTo("Žale d.o.o.");
  }

  @Test
  @DisplayName("AC-008-03 the export without credentials is refused with 401 and no data")
  void ac008_03_exportWithoutCredentialsIsRefused() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    assertThat(api.postRegistration(request).status()).isEqualTo(201);

    Response response = api.get(EXPORT);

    assertThat(response.status()).as(response.toString()).isEqualTo(401);
    assertThat(response.body())
        .doesNotContain((String) request.get("email"))
        .doesNotContain("Novak");
  }

  @Test
  @DisplayName("AC-008-04 the export with a wrong username or password is refused with 401")
  void ac008_04_exportWithWrongCredentialsIsRefused() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    assertThat(api.postRegistration(request).status()).isEqualTo(201);

    Response wrongPassword = api.get(EXPORT, ORGANIZER_USERNAME, "wrong-" + ORGANIZER_PASSWORD);
    Response wrongUser = api.get(EXPORT, "someone-else", ORGANIZER_PASSWORD);

    for (Response response : List.of(wrongPassword, wrongUser)) {
      assertThat(response.status()).as(response.toString()).isEqualTo(401);
      assertThat(response.body()).doesNotContain((String) request.get("email"));
    }
  }

  @Test
  @DisplayName("AC-008-05 text starting with = + - @ is exported as text, not as a formula")
  void ac008_05_formulaLikeTextStaysText() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("firstName", "=1+1");
    request.put("lastName", "+cmd|' /C calc'!A0");
    request.put("organization", "-2+3");
    Map<String, Object> second = Registrations.validExternal();
    second.put("firstName", "@SUM(A1:A2)");
    assertThat(api.postRegistration(request).status()).isEqualTo(201);
    assertThat(api.postRegistration(second).status()).isEqualTo(201);

    Response response = export();

    assertThat(response.status()).as(response.toString()).isEqualTo(200);
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.bytes()))) {
      Sheet sheet = workbook.getSheet("Registrations");
      for (Row row : sheet) {
        for (Cell cell : row) {
          assertThat(cell.getCellType())
              .as("cell %s", cell.getAddress())
              .isNotEqualTo(CellType.FORMULA);
        }
      }
      Row first = sheet.getRow(1);
      assertThat(first.getCell(3).getStringCellValue()).isEqualTo("=1+1");
      assertThat(first.getCell(4).getStringCellValue()).isEqualTo("+cmd|' /C calc'!A0");
      assertThat(first.getCell(6).getStringCellValue()).isEqualTo("-2+3");
      assertThat(sheet.getRow(2).getCell(3).getStringCellValue()).isEqualTo("@SUM(A1:A2)");
    }
  }

  @Test
  @DisplayName("AC-008-06 the export holds only registration data")
  void ac008_06_exportHoldsOnlyRegistrationData() throws Exception {
    assertThat(api.postRegistration(Registrations.validExternal()).status()).isEqualTo(201);

    Response response = export();

    assertThat(response.status()).as(response.toString()).isEqualTo(200);
    List<List<String>> rows = rows(response);
    assertThat(rows.get(0)).containsExactlyElementsOf(COLUMNS);
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.bytes()))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
    }
    String all = rows.toString();
    assertThat(all)
        .doesNotContain(Registrations.TEST_MODE_TOKEN)
        .doesNotContain(ORGANIZER_PASSWORD)
        .doesNotContain(POSTGRES.getPassword())
        .doesNotContainIgnoringCase("recaptcha")
        .doesNotContain("jdbc:");
  }
}
