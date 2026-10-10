package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.AcceptedRegistration;

/** Port: renders registrations as an Excel workbook (US-008). */
public interface WorkbookWriter {

  /** One header row, then one row per registration; registration data only (SR-07). */
  byte[] write(List<AcceptedRegistration> registrations);
}
