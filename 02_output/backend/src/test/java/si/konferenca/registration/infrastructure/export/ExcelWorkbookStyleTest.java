package si.konferenca.registration.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Covers AC-008-01: every export cell is quote-prefixed text (phase 5 survivors). */
class ExcelWorkbookStyleTest {

  @Test
  void AC_008_01_everyCellIsQuotePrefixedText() throws Exception {
    byte[] xlsx =
        new ExcelWorkbookWriter().write(List.of(ExcelWorkbookWriterTest.external("=1+1")));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = workbook.getSheet("Registrations");
      for (Row row : sheet) {
        row.forEach(cell -> assertThat(cell.getCellStyle().getQuotePrefixed()).isTrue());
      }
    }
  }
}
