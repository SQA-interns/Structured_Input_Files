package si.konferenca.registration.infrastructure.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import si.konferenca.registration.application.WorkbookWriter;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.Timestamps;

/** Writes sheet Registrations with string cells only (excel-export.md, SR-07). */
public class ExcelWorkbookWriter implements WorkbookWriter {

  private static final List<String> HEADER =
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
          "Options",
          "Consents");

  @Override
  public byte[] write(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      CellStyle text = workbook.createCellStyle();
      text.setQuotePrefixed(true);
      writeRow(sheet.createRow(0), HEADER, text);
      int index = 1;
      for (Registration registration : registrations) {
        writeRow(sheet.createRow(index++), cells(registration), text);
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("export workbook could not be written", e);
    }
  }

  private static List<String> cells(Registration r) {
    return List.of(
        r.id().toString(),
        r.type().name(),
        Timestamps.format(r.registeredAt()),
        r.firstName(),
        r.lastName(),
        r.email(),
        orEmpty(r.organization()),
        orEmpty(r.studyInstitution()),
        orEmpty(r.studyProgramme()),
        orEmpty(r.studentId()),
        r.options().stream().map(ConferenceOption::id).collect(Collectors.joining(", ")),
        r.consents().stream()
            .map(c -> c.id() + " (" + Timestamps.format(c.givenAt()) + ")")
            .collect(Collectors.joining(", ")));
  }

  private static void writeRow(Row row, List<String> values, CellStyle text) {
    for (int c = 0; c < values.size(); c++) {
      Cell cell = row.createCell(c);
      cell.setCellValue(values.get(c));
      cell.setCellStyle(text);
    }
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
