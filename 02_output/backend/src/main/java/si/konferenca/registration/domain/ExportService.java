package si.konferenca.registration.domain;

/** Use case: export all stored registrations (US-008, BR-08). */
public class ExportService {

  private final RegistrationStore store;
  private final RegistrationExporter exporter;

  public ExportService(RegistrationStore store, RegistrationExporter exporter) {
    this.store = store;
    this.exporter = exporter;
  }

  public byte[] exportAll() {
    return exporter.export(store.findAll());
  }
}
