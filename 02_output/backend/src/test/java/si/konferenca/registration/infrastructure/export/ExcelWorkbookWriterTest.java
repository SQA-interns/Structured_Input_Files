package si.konferenca.registration.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class ExcelWorkbookWriterTest {

  static final Instant T1 = Instant.parse("2026-10-19T10:00:00.100Z");
  static final Instant T2 = Instant.parse("2026-10-19T11:00:00Z");

  static Registration external(String firstName) {
    return new Registration(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        RegistrationType.EXTERNAL,
        T1,
        firstName,
        "Šuštaršič",
        "ana@example.com",
        "IJS ČŠŽ",
        null,
        null,
        null,
        List.of(
            new ConferenceOption("ws-a", "A", OptionCategory.WORKSHOP, true, null),
            new ConferenceOption("ev-b", "B", OptionCategory.EVENT, true, null)),
        List.of(new GivenConsent("privacy", T1), new GivenConsent("photos", T1)));
  }

  static Registration student() {
    return new Registration(
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        RegistrationType.STUDENT,
        T2,
        "Luka",
        "Kranjc",
        "luka@example.com",
        null,
        "Univerza",
        "Informatika",
        "6321",
        List.of(),
        List.of(new GivenConsent("privacy", T2)));
  }

  static List<List<String>> rows(byte[] xlsx, List<CellType> types) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = workbook.getSheet("Registrations");
      assertThat(sheet).isNotNull();
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (int c = 0; c < 12; c++) {
          Cell cell = row.getCell(c);
          if (cell != null) {
            types.add(cell.getCellType());
          }
          cells.add(cell == null ? "" : cell.getStringCellValue());
        }
        rows.add(cells);
      }
      return rows;
    }
  }

  @Test
  void AC_008_01_workbookHasHeaderAndOneStringRowPerRegistration() throws Exception {
    List<CellType> types = new ArrayList<>();

    List<List<String>> rows =
        rows(new ExcelWorkbookWriter().write(List.of(external("Žiga"), student())), types);

    assertThat(rows).hasSize(3);
    assertThat(rows.get(0))
        .containsExactly(
            "Registration ID",
            "Type",
            "Registered at",
            "First name",
            "Last name",
            "Email",
            "Organization / institution",
            "Study institution",
            "Study programme",
            "Student ID",
            "Options",
            "Consents");
    assertThat(rows.get(1))
        .containsExactly(
            "00000000-0000-0000-0000-000000000001",
            "EXTERNAL",
            "2026-10-19T10:00:00.100Z",
            "Žiga",
            "Šuštaršič",
            "ana@example.com",
            "IJS ČŠŽ",
            "",
            "",
            "",
            "ws-a, ev-b",
            "privacy (2026-10-19T10:00:00.100Z), photos (2026-10-19T10:00:00.100Z)");
    assertThat(rows.get(2))
        .containsExactly(
            "00000000-0000-0000-0000-000000000002",
            "STUDENT",
            "2026-10-19T11:00:00.000Z",
            "Luka",
            "Kranjc",
            "luka@example.com",
            "",
            "Univerza",
            "Informatika",
            "6321",
            "",
            "privacy (2026-10-19T11:00:00.000Z)");
    assertThat(types).containsOnly(CellType.STRING);
  }

  @Test
  void AC_008_01_formulaLikeTextStaysPlainText() throws Exception {
    List<CellType> types = new ArrayList<>();

    List<List<String>> rows =
        rows(new ExcelWorkbookWriter().write(List.of(external("=HYPERLINK(\"x\")"))), types);

    assertThat(rows.get(1).get(3)).isEqualTo("=HYPERLINK(\"x\")");
    assertThat(types).doesNotContain(CellType.FORMULA);
  }

  @Test
  void AC_008_01_emptyExportHasOnlyTheHeader() throws Exception {
    assertThat(rows(new ExcelWorkbookWriter().write(List.of()), new ArrayList<>())).hasSize(1);
  }
}
