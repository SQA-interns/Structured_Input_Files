package si.konferenca.registration.application;

/** Use case: the organizer export, one row per registration by registration time (US-008). */
public class ExportRegistrations {

  public ExportRegistrations(RegistrationReader reader, WorkbookWriter writer) {}

  public byte[] export() {
    throw new UnsupportedOperationException("not implemented");
  }
}
