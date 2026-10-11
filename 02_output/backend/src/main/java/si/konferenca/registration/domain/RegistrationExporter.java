package si.konferenca.registration.domain;

import java.util.List;

/** Port: turns registrations into a workbook for organizers (US-008). */
public interface RegistrationExporter {

  byte[] export(List<Registration> registrations);
}
