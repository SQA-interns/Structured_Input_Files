package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Port: writes the export workbook (excel-export.md). */
public interface WorkbookWriter {

  byte[] write(List<Registration> registrations);
}
