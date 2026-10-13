package si.konferenca.registration.adapter.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.Ports.WorkbookWriter;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;

/**
 * The `.xlsx` export of spec §6: sheet "Registrations", a header row, one row per registration.
 * Every cell is a string cell, so text is unchanged and never evaluated as a formula.
 */
@Component
public class PoiWorkbookWriter implements WorkbookWriter {

  static final List<String> HEADERS =
      List.of(
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
          "Workshops",
          "Events",
          "Meals",
          "Other activities",
          "Consents");

  @Override
  public byte[] write(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      fill(sheet.createRow(0), HEADERS);
      int index = 1;
      for (Registration registration : registrations) {
        fill(sheet.createRow(index++), cells(registration));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Workbook could not be written", e);
    }
  }

  static List<String> cells(Registration r) {
    List<String> cells = new ArrayList<>();
    cells.add(r.id().toString());
    cells.add(r.type().name());
    cells.add(Registration.format(r.registeredAt()));
    cells.add(r.firstName());
    cells.add(r.lastName());
    cells.add(r.email());
    cells.add(r.organization());
    cells.add(r.studyInstitution());
    cells.add(r.studyProgramme());
    cells.add(r.studentId());
    for (OptionCategory category : OptionCategory.values()) {
      cells.add(
          r.options().stream()
              .filter(o -> o.category() == category)
              .map(Registration.SelectedOption::name)
              .collect(Collectors.joining("; ")));
    }
    cells.add(
        r.consents().stream()
            .map(c -> c.id() + " (" + Registration.format(c.givenAt()) + ")")
            .collect(Collectors.joining("; ")));
    return cells;
  }

  private static void fill(Row row, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      String value = values.get(i);
      row.createCell(i).setCellValue(value == null ? "" : value);
    }
  }
}
