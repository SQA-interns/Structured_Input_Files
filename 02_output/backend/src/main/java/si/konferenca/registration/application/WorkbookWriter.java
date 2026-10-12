package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Port: the Excel workbook of the export (US-008). */
public interface WorkbookWriter {

  byte[] write(List<Registration> registrations);
}
