package si.konferenca.registration.infra;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import si.konferenca.registration.domain.Registration;

/**
 * The organizer workbook (specification section 8): one sheet, one row per registration, string
 * cells only so that no text is ever evaluated as a formula; no internal fields (SR-07).
 */
@Component
public class ExcelWriter {

  static final List<String> COLUMNS =
      List.of(
          "Registration ID",
          "Type",
          "First name",
          "Last name",
          "Email",
          "Organization",
          "Study institution",
          "Study programme",
          "Student ID",
          "Options",
          "Consents",
          "Registered at");

  public byte[] write(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      writeRow(sheet.createRow(0), COLUMNS);
      int index = 1;
      for (Registration registration : registrations) {
        writeRow(sheet.createRow(index++), values(registration));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Workbook could not be written", e);
    }
  }

  static List<String> values(Registration registration) {
    return List.of(
        registration.getId().toString(),
        registration.getType().name(),
        registration.getFirstName(),
        registration.getLastName(),
        registration.getEmail(),
        orEmpty(registration.getOrganization()),
        orEmpty(registration.getStudyInstitution()),
        orEmpty(registration.getStudyProgramme()),
        orEmpty(registration.getStudentId()),
        registration.getOptions().stream()
            .map(o -> o.optionName())
            .collect(Collectors.joining("; ")),
        registration.getConsents().stream()
            .map(c -> c.consentId() + " (" + c.givenAt() + ")")
            .collect(Collectors.joining("; ")),
        registration.getRegisteredAt().toString());
  }

  private static void writeRow(Row row, List<String> values) {
    for (int column = 0; column < values.size(); column++) {
      row.createCell(column).setCellValue(values.get(column));
    }
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
