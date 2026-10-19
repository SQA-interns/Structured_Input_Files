package si.konferenca.registration.application;

import java.util.Comparator;
import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Use case: the organizer export, one row per registration by registration time (US-008). */
public class ExportRegistrations {

  private final RegistrationReader reader;
  private final WorkbookWriter writer;

  public ExportRegistrations(RegistrationReader reader, WorkbookWriter writer) {
    this.reader = reader;
    this.writer = writer;
  }

  public byte[] export() {
    List<Registration> registrations =
        reader.findAll().stream().sorted(Comparator.comparing(Registration::registeredAt)).toList();
    return writer.write(registrations);
  }
}
