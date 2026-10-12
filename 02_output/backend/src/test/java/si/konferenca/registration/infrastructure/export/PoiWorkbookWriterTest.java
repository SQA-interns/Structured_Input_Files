package si.konferenca.registration.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.SelectedOption;
import si.konferenca.registration.infrastructure.TestRegistrations;

class PoiWorkbookWriterTest {

  private static Sheet sheet(byte[] bytes) throws Exception {
    return new XSSFWorkbook(new ByteArrayInputStream(bytes)).getSheet("Registrations");
  }

  @Test
  void writesHeaderOnlyForNoRegistrations() throws Exception {
    Sheet sheet = sheet(new PoiWorkbookWriter().write(List.of()));

    assertThat(sheet.getLastRowNum()).isZero();
    assertThat(sheet.getRow(0).getLastCellNum()).isEqualTo((short) 16);
    assertThat(sheet.getRow(0).getCell(15).getStringCellValue()).isEqualTo("Consent given at");
  }

  @Test
  void writesOneStringRowPerRegistrationWithOptionsPerCategory() throws Exception {
    Sheet sheet =
        sheet(
            new PoiWorkbookWriter()
                .write(
                    List.of(
                        TestRegistrations.external(
                            "=1+1",
                            List.of(
                                new SelectedOption("d", "Dinner", Category.EVENT),
                                new SelectedOption("l", "Lunch", Category.MEAL))),
                        TestRegistrations.student())));

    Row external = sheet.getRow(1);
    assertThat(external.getCell(0).getStringCellValue())
        .isEqualTo("11111111-1111-1111-1111-111111111111");
    assertThat(external.getCell(1).getStringCellValue()).isEqualTo("EXTERNAL");
    assertThat(external.getCell(2).getStringCellValue()).isEqualTo("2026-10-12T10:00:00Z");
    assertThat(external.getCell(3).getCellType()).isEqualTo(CellType.STRING);
    assertThat(external.getCell(3).getStringCellValue()).isEqualTo("=1+1");
    assertThat(external.getCell(6).getStringCellValue()).isEqualTo("Acme");
    assertThat(external.getCell(7)).isNull();
    assertThat(external.getCell(10)).isNull();
    assertThat(external.getCell(11).getStringCellValue()).isEqualTo("Dinner");
    assertThat(external.getCell(12).getStringCellValue()).isEqualTo("Lunch");
    assertThat(external.getCell(14).getStringCellValue()).isEqualTo("I agree.");

    Row student = sheet.getRow(2);
    assertThat(student.getCell(6)).isNull();
    assertThat(student.getCell(9).getStringCellValue()).isEqualTo("123");
    assertThat(student.getCell(10).getStringCellValue()).isEqualTo("Workshop; Workshop 2");
    assertThat(student.getCell(13).getStringCellValue()).isEqualTo("Career fair");
    assertThat(student.getCell(15).getStringCellValue()).isEqualTo("2026-10-12T10:01:00Z");
  }
}
