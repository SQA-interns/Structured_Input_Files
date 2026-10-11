package si.konferenca.registration.adapter.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.adapter.Fixtures;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;

class ExcelRegistrationExporterTest {

  @Test
  void writesHeaderAndOneStringRowPerRegistration() throws Exception {
    byte[] xlsx =
        new ExcelRegistrationExporter().export(List.of(Fixtures.external(), Fixtures.student()));

    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = wb.getSheet("Registrations");
      assertThat(sheet.getLastRowNum()).isEqualTo(2);
      Row header = sheet.getRow(0);
      for (int i = 0; i < ExcelRegistrationExporter.HEADERS.size(); i++) {
        assertThat(header.getCell(i).getStringCellValue())
            .isEqualTo(ExcelRegistrationExporter.HEADERS.get(i));
      }
      Row ext = sheet.getRow(1);
      assertThat(ext.getCell(2).getStringCellValue()).isEqualTo("External participant");
      assertThat(ext.getCell(3).getStringCellValue()).isEqualTo("Črt");
      assertThat(ext.getCell(10).getStringCellValue()).isEqualTo("Delavnica");
      assertThat(ext.getCell(11).getStringCellValue()).isEmpty();
      assertThat(ext.getCell(12).getStringCellValue()).isEqualTo("Kosilo; Večerja");
      assertThat(ext.getCell(14).getStringCellValue()).isEqualTo("privacy (2026-10-11T08:30:00Z)");
      Row stu = sheet.getRow(2);
      assertThat(stu.getCell(2).getStringCellValue()).isEqualTo("Student");
      assertThat(stu.getCell(6).getStringCellValue()).isEmpty();
      assertThat(stu.getCell(9).getStringCellValue()).isEqualTo("632");
    }
  }

  @Test
  void formulaLikeInputStaysText() throws Exception {
    Registration r = Fixtures.external();
    Registration evil =
        new Registration(
            r.id(),
            r.type(),
            r.registeredAt(),
            new Participant("=1+1", "@SUM(A1)", "a@b.si", "-2", null, null, null),
            List.of(),
            List.of());

    byte[] xlsx = new ExcelRegistrationExporter().export(List.of(evil));

    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Row row = wb.getSheet("Registrations").getRow(1);
      assertThat(row.getCell(3).getCellType()).isEqualTo(CellType.STRING);
      assertThat(row.getCell(3).getStringCellValue()).isEqualTo("=1+1");
      assertThat(row.getCell(4).getStringCellValue()).isEqualTo("@SUM(A1)");
    }
  }

  @Test
  void valuesFollowTheHeaderOrder() {
    List<String> values = ExcelRegistrationExporter.values(Fixtures.student());

    assertThat(values).hasSize(ExcelRegistrationExporter.HEADERS.size());
    assertThat(values.get(0)).isEqualTo("66666666-7777-8888-9999-000000000000");
    assertThat(values.get(1)).isEqualTo("2026-10-11T08:30:00Z");
    assertThat(values.subList(7, 10)).containsExactly("UL", "RI", "632");
    assertThat(values.get(14)).isEmpty();
  }
}
