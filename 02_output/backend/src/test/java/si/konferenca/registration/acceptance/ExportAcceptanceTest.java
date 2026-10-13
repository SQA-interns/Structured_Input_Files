package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.node.ObjectNode;

/** US-008 Excel export for organizers. */
class ExportAcceptanceTest extends AcceptanceTestBase {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Test
  @DisplayName(
      "AC-008-01 organizer export is an xlsx workbook with one row per registration and its data")
  void ac00801ExportContainsOneRowPerRegistration() throws IOException {
    assertAccepted(api.postRegistration(Submissions.validExternal()));
    assertAccepted(api.postRegistration(Submissions.validStudent()));

    Api.Response response = api.exportAsOrganizer();

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.header("Content-Type")).startsWith(XLSX);
    List<List<String>> rows = dataRows(response.bytes());
    assertThat(rows).as("one row per registration").hasSize(2);

    List<String> external = rowContaining(rows, "ana.novak@example.si");
    assertThat(external)
        .contains("EXTERNAL", "Ana", "Novak", "ana.novak@example.si", "Institut Primer");
    assertThat(String.join("|", external))
        .contains("Workshop A", "Workshop for externals", "Evening event", "Lunch", "City tour")
        .contains("privacy");
    assertThat(external).anyMatch(c -> c.matches(".*\\d{4}-\\d{2}-\\d{2}.*"));

    List<String> student = rowContaining(rows, "luka.kranjc@student.example.si");
    assertThat(student)
        .contains(
            "STUDENT",
            "Luka",
            "Kranjc",
            "Univerza v Ljubljani",
            "Računalništvo in informatika",
            "63210001");
    assertThat(String.join("|", student)).contains("Student meetup").contains("privacy");
    assertThat(student).anyMatch(c -> c.matches(".*\\d{4}-\\d{2}-\\d{2}.*"));
  }

  @ParameterizedTest(name = "AC-008-02 credentials [{0}:{1}] are refused")
  @CsvSource(
      value = {
        "NONE, NONE",
        "organizer, wrong-password-123456",
        "intruder, acceptance-organizer-secret-42"
      },
      nullValues = "NONE")
  @DisplayName("AC-008-02 export without or with wrong credentials is refused with 401 and no data")
  void ac00802ExportRefusedWithoutValidCredentials(String user, String password) {
    assertAccepted(api.postRegistration(Submissions.validExternal()));

    Api.Response response = api.export(user, password, Map.of());

    assertThat(response.status()).isEqualTo(401);
    assertNoRegistrationData(response);
  }

  @Test
  @DisplayName("AC-008-03 Slovenian characters appear unchanged in the workbook cells")
  void ac00803SlovenianCharactersInWorkbook() throws IOException {
    ObjectNode body = Submissions.validExternal();
    body.put("firstName", "Žiga Čeh");
    body.put("lastName", "Šuštaršič ČŠŽ");
    body.put("organization", "Fakulteta za računalništvo, čšž ČŠŽ");
    assertAccepted(api.postRegistration(body));

    Api.Response response = api.exportAsOrganizer();

    assertThat(response.status()).isEqualTo(200);
    List<List<String>> rows = dataRows(response.bytes());
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0))
        .contains("Žiga Čeh", "Šuštaršič ČŠŽ", "Fakulteta za računalništvo, čšž ČŠŽ");
  }

  @Test
  @DisplayName(
      "AC-008-04 with HTTPS-only access, valid credentials over plain HTTP from a remote client are refused")
  void ac00804PlainHttpFromRemoteClientRefused() {
    assertAccepted(api.postRegistration(Submissions.validExternal()));

    Api.Response remote =
        api.export(
            AcceptanceEnvironment.ORGANIZER_USERNAME,
            AcceptanceEnvironment.ORGANIZER_PASSWORD,
            Map.of("X-Forwarded-For", "203.0.113.10", "X-Forwarded-Proto", "http"));

    assertThat(remote.status()).as("refused").isIn(401, 403);
    assertNoRegistrationData(remote);

    // Control: the same credentials from localhost are accepted, so the refusal is due to the
    // remote plain-HTTP client and not to the credentials.
    assertThat(api.exportAsOrganizer().status()).isEqualTo(200);
  }

  private static void assertNoRegistrationData(Api.Response response) {
    assertThat(String.valueOf(response.header("Content-Type"))).doesNotStartWith(XLSX);
    assertThat(response.text()).doesNotContain("Novak").doesNotContain("ana.novak@example.si");
    byte[] bytes = response.bytes();
    boolean zip = bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K';
    assertThat(zip).as("body is a workbook").isFalse();
  }

  /** Every row after the header row, as the formatted text of each cell. */
  private static List<List<String>> dataRows(byte[] workbook) throws IOException {
    try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(workbook))) {
      Sheet sheet = book.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        if (row.getRowNum() == sheet.getFirstRowNum()) {
          continue;
        }
        List<String> cells = new ArrayList<>();
        for (Cell cell : row) {
          cells.add(formatter.formatCellValue(cell));
        }
        rows.add(cells);
      }
      return rows;
    }
  }

  private static List<String> rowContaining(List<List<String>> rows, String value) {
    return rows.stream()
        .filter(r -> r.contains(value))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no row contains " + value));
  }
}
