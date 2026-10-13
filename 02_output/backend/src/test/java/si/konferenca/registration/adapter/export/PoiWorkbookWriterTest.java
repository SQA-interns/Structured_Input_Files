package si.konferenca.registration.adapter.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.TestRegistrations;

/** US-008, spec §6: workbook layout and cell values. */
class PoiWorkbookWriterTest {

  @Test
  void workbookHasAHeaderRowAndOneStringRowPerRegistration() throws Exception {
    byte[] bytes =
        new PoiWorkbookWriter()
            .write(List.of(TestRegistrations.external(), TestRegistrations.student()));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheet("Registrations");
      assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3);
      Row header = sheet.getRow(0);
      for (int i = 0; i < PoiWorkbookWriter.HEADERS.size(); i++) {
        assertThat(header.getCell(i).getStringCellValue())
            .isEqualTo(PoiWorkbookWriter.HEADERS.get(i));
      }
      Row first = sheet.getRow(1);
      assertThat(first.getCell(0).getCellType()).isEqualTo(CellType.STRING);
      assertThat(first.getCell(3).getStringCellValue()).isEqualTo("Žiga");
      assertThat(first.getCell(7).getStringCellValue()).isEmpty();
      assertThat(sheet.getRow(2).getCell(9).getStringCellValue()).isEqualTo("63");
    }
  }

  @Test
  void cellsGroupOptionsByCategoryAndListConsents() {
    assertThat(PoiWorkbookWriter.cells(TestRegistrations.external()))
        .containsExactly(
            "0b8f3c1e-2d4a-4b6e-9f00-112233445566",
            "EXTERNAL",
            "2026-10-13T20:15:03.120Z",
            "Žiga",
            "Čeč",
            "ziga@example.si",
            "Šola <b>",
            null,
            null,
            null,
            "Workshop; Workshop 2",
            "Event",
            "",
            "",
            "privacy (2026-10-13T20:15:03.120Z)");
  }

  @Test
  void emptyExportHasOnlyTheHeader() throws Exception {
    try (XSSFWorkbook workbook =
        new XSSFWorkbook(new ByteArrayInputStream(new PoiWorkbookWriter().write(List.of())))) {
      assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(1);
    }
  }
}
