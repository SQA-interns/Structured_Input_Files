package si.konferenca.registration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.AcceptedRegistration;

class PoiWorkbookWriterTest {

  @Test
  void writesHeaderAndOneStringRowPerRegistration() throws Exception {
    AcceptedRegistration formula =
        new AcceptedRegistration(
            Registrations.REFERENCE,
            Registrations.AT,
            Registrations.external().type(),
            new AcceptedRegistration.Participant("=1+1", "N", "e@x.si", "O", null, null, null),
            List.of(),
            List.of());

    byte[] xlsx =
        new PoiWorkbookWriter()
            .write(List.of(Registrations.external(), Registrations.student(), formula));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      Sheet sheet = workbook.getSheet("Registrations");
      assertThat(sheet.getLastRowNum()).isEqualTo(3);
      Row header = sheet.getRow(0);
      for (int c = 0; c < PoiWorkbookWriter.COLUMNS.size(); c++) {
        assertThat(header.getCell(c).getStringCellValue())
            .isEqualTo(PoiWorkbookWriter.COLUMNS.get(c));
      }
      Row student = sheet.getRow(2);
      assertThat(student.getCell(6).getStringCellValue()).isEmpty();
      assertThat(student.getCell(9).getStringCellValue()).isEqualTo("E1");
      Row injected = sheet.getRow(3);
      assertThat(injected.getCell(3).getCellType()).isEqualTo(CellType.STRING);
      assertThat(injected.getCell(3).getStringCellValue()).isEqualTo("=1+1");
    }
  }

  @Test
  void valuesFollowTheColumns() {
    List<String> values = PoiWorkbookWriter.values(Registrations.external());

    assertThat(values).hasSize(PoiWorkbookWriter.COLUMNS.size());
    assertThat(values)
        .containsExactly(
            Registrations.REFERENCE.toString(),
            "2026-10-11T08:00:00.123Z",
            "EXTERNAL",
            "Čedomir",
            "Žagar",
            "c@example.si",
            "IJS",
            null,
            null,
            null,
            "AI, Security",
            "",
            "Lunch",
            "",
            "data (2026-10-11T08:00:00.123Z)");
    assertThat(PoiWorkbookWriter.values(Registrations.student()).get(14))
        .isEqualTo("data (2026-10-11T08:00:00.123Z); photos (2026-10-11T08:00:00.123Z)");
  }
}
