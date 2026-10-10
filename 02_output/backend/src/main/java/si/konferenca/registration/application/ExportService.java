package si.konferenca.registration.application;

import org.springframework.transaction.annotation.Transactional;

/** Registration export for organizers (US-008, BR-08). */
public class ExportService {

  private final RegistrationStore store;
  private final WorkbookWriter writer;

  public ExportService(RegistrationStore store, WorkbookWriter writer) {
    this.store = store;
    this.writer = writer;
  }

  /** The current registrations as an .xlsx workbook. */
  @Transactional(readOnly = true)
  public byte[] exportWorkbook() {
    return writer.write(store.findAllOrderedBySubmission());
  }
}
