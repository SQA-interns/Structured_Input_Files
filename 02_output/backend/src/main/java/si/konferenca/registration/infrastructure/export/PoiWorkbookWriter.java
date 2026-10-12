package si.konferenca.registration.infrastructure.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import si.konferenca.registration.application.WorkbookWriter;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;

/**
 * Sheet {@code Registrations} with the columns of specification S9. Every value is a string cell,
 * never a formula (AC-008-05); only registration data is written (SR-07).
 */
public class PoiWorkbookWriter implements WorkbookWriter {

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

  @Override
  public byte[] write(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      writeRow(sheet.createRow(0), COLUMNS);
      int index = 1;
      for (Registration r : registrations) {
        writeRow(sheet.createRow(index++), values(r));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Workbook could not be written", e);
    }
  }

  private static List<String> values(Registration r) {
    return List.of(
        r.id().toString(),
        r.type().name(),
        time(r.submittedAt()),
        r.firstName(),
        r.lastName(),
        r.email(),
        orEmpty(r.organization()),
        orEmpty(r.studyInstitution()),
        orEmpty(r.studyProgramme()),
        orEmpty(r.studentId()),
        names(r.selectedOptions(), Category.WORKSHOP),
        names(r.selectedOptions(), Category.EVENT),
        names(r.selectedOptions(), Category.MEAL),
        names(r.selectedOptions(), Category.OTHER),
        r.consent().text(),
        time(r.consent().givenAt()));
  }

  private static void writeRow(Row row, List<String> values) {
    for (int c = 0; c < values.size(); c++) {
      String value = values.get(c);
      if (!value.isEmpty()) {
        row.createCell(c).setCellValue(value);
      }
    }
  }

  private static String names(List<SelectedOption> options, Category category) {
    return String.join(
        "; ",
        options.stream().filter(o -> o.category() == category).map(SelectedOption::name).toList());
  }

  private static String time(Instant instant) {
    return DateTimeFormatter.ISO_INSTANT.format(instant);
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
