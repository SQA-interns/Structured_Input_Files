package si.konferenca.registration.application;

import org.springframework.stereotype.Service;
import si.konferenca.registration.application.Ports.RegistrationStore;
import si.konferenca.registration.application.Ports.WorkbookWriter;

/** Use case: every stored registration as an Excel workbook, oldest first (US-008). */
@Service
public class ExportRegistrations {

  private final RegistrationStore store;
  private final WorkbookWriter writer;

  public ExportRegistrations(RegistrationStore store, WorkbookWriter writer) {
    this.store = store;
    this.writer = writer;
  }

  public byte[] export() {
    return writer.write(store.findAllOldestFirst());
  }
}
