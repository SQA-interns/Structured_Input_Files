package si.konferenca.registration.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.persistence.RegistrationStore;

class RegistrationExportTest {

  static final Instant AT = Instant.parse("2026-10-13T08:00:00.120Z");

  static Registration external(String firstName) {
    return new Registration(
        UUID.fromString("6f1c1b7e-2f7a-4c55-9d0e-3c1f7c2b9a10"),
        AT,
        RegistrationType.EXTERNAL,
        firstName,
        "Novak",
        "a@b.co",
        "Acme",
        null,
        null,
        null,
        List.of(
            new Registration.SelectedOption("ws", "Workshop", OptionCategory.WORKSHOP),
            new Registration.SelectedOption("ml", "Lunch", OptionCategory.MEAL)),
        List.of(
            new Registration.GivenConsent("privacy", "P", AT),
            new Registration.GivenConsent("photos", "F", AT)));
  }

  static Registration student() {
    return new Registration(
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        AT,
        RegistrationType.STUDENT,
        "Bor",
        "Kranjc",
        "b@c.co",
        null,
        "Uni",
        "CS",
        "123",
        List.of(),
        List.of());
  }

  @Test
  void rowHasEveryColumnInHeaderOrder() {
    assertThat(RegistrationExport.row(external("Ana")))
        .containsExactly(
            "6f1c1b7e-2f7a-4c55-9d0e-3c1f7c2b9a10",
            "EXTERNAL",
            "2026-10-13T08:00:00.120Z",
            "Ana",
            "Novak",
            "a@b.co",
            "Acme",
            "",
            "",
            "",
            "Workshop; Lunch",
            "privacy given 2026-10-13T08:00:00.120Z; photos given 2026-10-13T08:00:00.120Z");
    assertThat(RegistrationExport.row(student()))
        .containsExactly(
            "00000000-0000-0000-0000-000000000002",
            "STUDENT",
            "2026-10-13T08:00:00.120Z",
            "Bor",
            "Kranjc",
            "b@c.co",
            "",
            "Uni",
            "CS",
            "123",
            "",
            "");
    assertThat(RegistrationExport.HEADER).hasSize(12);
  }

  @Test
  void workbookHasHeaderAndOneStringRowPerRegistration() throws Exception {
    RegistrationStore store = mock(RegistrationStore.class);
    when(store.findAllInOrder()).thenReturn(List.of(external("=1+1"), student()));

    byte[] bytes = new RegistrationExport(store).workbook();

    try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      assertThat(book.getNumberOfSheets()).isEqualTo(1);
      Sheet sheet = book.getSheet("Registrations");
      assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3);
      assertThat(values(sheet.getRow(0))).isEqualTo(RegistrationExport.HEADER);
      assertThat(values(sheet.getRow(1))).isEqualTo(RegistrationExport.row(external("=1+1")));
      assertThat(values(sheet.getRow(2))).isEqualTo(RegistrationExport.row(student()));
      for (Cell cell : sheet.getRow(1)) {
        assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
      }
    }
  }

  @Test
  void emptyExportHasOnlyTheHeader() throws Exception {
    RegistrationStore store = mock(RegistrationStore.class);
    when(store.findAllInOrder()).thenReturn(List.of());

    try (XSSFWorkbook book =
        new XSSFWorkbook(new ByteArrayInputStream(new RegistrationExport(store).workbook()))) {
      assertThat(book.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(1);
    }
  }

  private static List<String> values(Row row) {
    List<String> values = new ArrayList<>();
    for (Cell cell : row) {
      values.add(cell.getStringCellValue());
    }
    return values;
  }
}
