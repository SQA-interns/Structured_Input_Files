package si.konferenca.registration.infrastructure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import si.konferenca.registration.application.WorkbookWriter;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.Participant;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;
import si.konferenca.registration.domain.OptionCategory;

/**
 * Apache POI adapter: sheet "Registrations", the columns of spec section 7, every cell a string
 * cell so no participant input is evaluated as a formula.
 */
public class PoiWorkbookWriter implements WorkbookWriter {

  static final List<String> COLUMNS =
      List.of(
          "Reference",
          "Submitted at",
          "Type",
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
          "Consents");

  private static final int COLUMN_WIDTH = 30 * 256;

  @Override
  public byte[] write(List<AcceptedRegistration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      writeRow(sheet.createRow(0), COLUMNS);
      int index = 1;
      for (AcceptedRegistration registration : registrations) {
        writeRow(sheet.createRow(index++), values(registration));
      }
      for (int c = 0; c < COLUMNS.size(); c++) {
        sheet.setColumnWidth(c, COLUMN_WIDTH);
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("workbook could not be written", e);
    }
  }

  static List<String> values(AcceptedRegistration r) {
    Participant p = r.participant();
    List<String> values = new ArrayList<>();
    values.add(r.reference().toString());
    values.add(r.submittedAt().toString());
    values.add(r.type().name());
    values.add(p.firstName());
    values.add(p.lastName());
    values.add(p.email());
    values.add(p.organization());
    values.add(p.studyInstitution());
    values.add(p.studyProgramme());
    values.add(p.studentId());
    for (OptionCategory category : OptionCategory.values()) {
      values.add(
          r.options().stream()
              .filter(o -> o.category() == category)
              .map(SelectedOption::name)
              .collect(Collectors.joining(", ")));
    }
    values.add(
        r.consents().stream()
            .map(c -> c.id() + " (" + c.givenAt() + ")")
            .collect(Collectors.joining("; ")));
    return values;
  }

  private static void writeRow(Row row, List<String> values) {
    for (int c = 0; c < values.size(); c++) {
      String value = values.get(c);
      row.createCell(c).setCellValue(value == null ? "" : value);
    }
  }
}
