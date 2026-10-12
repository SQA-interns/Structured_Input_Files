package si.konferenca.registration.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.ParticipantDetails;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class ExcelWriterTest {

  private static List<List<String>> read(byte[] xlsx) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getSheetName()).isEqualTo("Registrations");
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (Cell cell : row) {
          assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
          cells.add(cell.getStringCellValue());
        }
        rows.add(cells);
      }
      return rows;
    }
  }

  @Test
  void writesHeaderAndOneRowPerRegistration() throws Exception {
    List<List<String>> rows =
        read(new ExcelWriter().write(List.of(Registrations.external(), Registrations.student())));

    assertThat(rows).hasSize(3);
    assertThat(rows.get(0)).isEqualTo(ExcelWriter.COLUMNS);
    assertThat(rows.get(1))
        .containsExactly(
            Registrations.EXTERNAL_ID.toString(),
            "EXTERNAL",
            "Ana",
            "Novak",
            "ana@example.org",
            "Institut",
            "",
            "",
            "",
            "Workshop A; Lunch",
            "privacy (2026-10-12T10:15:30.123456Z); photo (2026-10-12T10:15:30.123456Z)",
            "2026-10-12T10:15:30.123456Z");
    assertThat(rows.get(2))
        .containsExactly(
            Registrations.STUDENT_ID.toString(),
            "STUDENT",
            "Luka",
            "Kranjc",
            "luka@example.org",
            "",
            "Univerza",
            "CS",
            "6320",
            "",
            "privacy (2026-10-12T10:15:30.123456Z)",
            "2026-10-12T10:15:30.123456Z");
  }

  @Test
  void keepsFormulaLikeTextAsText() throws Exception {
    Registration registration =
        new Registration(
            Registrations.EXTERNAL_ID,
            RegistrationType.EXTERNAL,
            new ParticipantDetails("=1+1", "@SUM(A1)", "a@b.org", "+x", null, null, null),
            Registrations.AT);

    List<List<String>> rows = read(new ExcelWriter().write(List.of(registration)));

    assertThat(rows.get(1)).contains("=1+1", "@SUM(A1)", "+x");
  }

  @Test
  void writesOnlyTheHeaderWithoutRegistrations() throws Exception {
    assertThat(read(new ExcelWriter().write(List.of()))).hasSize(1);
  }
}
