package si.konferenca.registration.infrastructure.export;

import java.util.List;
import si.konferenca.registration.application.WorkbookWriter;
import si.konferenca.registration.domain.Registration;

/** Writes sheet Registrations with string cells only (excel-export.md, SR-07). */
public class ExcelWorkbookWriter implements WorkbookWriter {

  @Override
  public byte[] write(List<Registration> registrations) {
    throw new UnsupportedOperationException("not implemented");
  }
}
