package si.konferenca.registration.adapter.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationExporter;

/**
 * Sheet "Registrations", one row per registration (specification section 8). Every value is a
 * string cell, so nothing is evaluated as a formula (SB-05); only registration data (SR-07).
 */
public final class ExcelRegistrationExporter implements RegistrationExporter {

  static final List<String> HEADERS =
      List.of(
          "Registration ID",
          "Registered at",
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

  @Override
  public byte[] export(List<Registration> registrations) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Registrations");
      write(sheet.createRow(0), HEADERS);
      int index = 1;
      for (Registration r : registrations) {
        write(sheet.createRow(index++), values(r));
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Workbook cannot be written", e);
    }
  }

  static List<String> values(Registration r) {
    Participant p = r.participant();
    return List.of(
        r.id().toString(),
        r.registeredAt().toString(),
        r.type().label(),
        p.firstName(),
        p.lastName(),
        p.email(),
        orEmpty(p.organization()),
        orEmpty(p.studyInstitution()),
        orEmpty(p.studyProgramme()),
        orEmpty(p.studentId()),
        options(r, OptionCategory.WORKSHOP),
        options(r, OptionCategory.EVENT),
        options(r, OptionCategory.MEAL),
        options(r, OptionCategory.OTHER),
        r.consents().stream()
            .map(c -> c.id() + " (" + c.givenAt() + ")")
            .collect(Collectors.joining("; ")));
  }

  private static String options(Registration r, OptionCategory category) {
    return r.options().stream()
        .filter(o -> o.category() == category)
        .map(o -> o.name())
        .collect(Collectors.joining("; "));
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  private static void write(Row row, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      row.createCell(i).setCellValue(values.get(i));
    }
  }
}
