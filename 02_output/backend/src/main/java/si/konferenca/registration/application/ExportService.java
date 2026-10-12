package si.konferenca.registration.application;

/** Use case: export every registration for organizers (US-008, BR-08). */
public class ExportService {

  private final RegistrationStore store;
  private final WorkbookWriter workbooks;

  public ExportService(RegistrationStore store, WorkbookWriter workbooks) {
    this.store = store;
    this.workbooks = workbooks;
  }

  public byte[] export() {
    return workbooks.write(store.findAllBySubmissionTime());
  }
}
