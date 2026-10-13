package si.konferenca.registration.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.persistence.RegistrationStore;

/** The Excel workbook of all registrations (spec section 10, US-008, SR-07). */
@Service
public class RegistrationExport {

  static final List<String> HEADER =
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

  private final RegistrationStore store;

  public RegistrationExport(RegistrationStore store) {
    this.store = store;
  }

  /** One header row, then one row per registration in order of registration time. */
  @Transactional(readOnly = true)
  public byte[] workbook() {
    List<Registration> registrations = store.findAllInOrder();
    try (XSSFWorkbook book = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = book.createSheet("Registrations");
      write(sheet.createRow(0), HEADER);
      int index = 1;
      for (Registration registration : registrations) {
        write(sheet.createRow(index++), row(registration));
      }
      book.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Workbook could not be written", e);
    }
  }

  static List<String> row(Registration r) {
    return List.of(
        r.id().toString(),
        r.type().name(),
        r.registeredAt().toString(),
        r.firstName(),
        r.lastName(),
        r.email(),
        orEmpty(r.organization()),
        orEmpty(r.studyInstitution()),
        orEmpty(r.studyProgramme()),
        orEmpty(r.studentId()),
        r.options().stream()
            .map(Registration.SelectedOption::name)
            .collect(Collectors.joining("; ")),
        r.consents().stream()
            .map(c -> c.id() + " given " + c.givenAt())
            .collect(Collectors.joining("; ")));
  }

  /** Every cell is a string cell, so no value is ever evaluated as a formula. */
  private static void write(Row row, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      row.createCell(i).setCellValue(values.get(i));
    }
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
