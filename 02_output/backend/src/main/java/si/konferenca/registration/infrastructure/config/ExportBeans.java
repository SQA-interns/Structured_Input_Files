package si.konferenca.registration.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.ExportRegistrations;
import si.konferenca.registration.application.RegistrationReader;
import si.konferenca.registration.application.WorkbookWriter;
import si.konferenca.registration.infrastructure.export.ExcelWorkbookWriter;

/** Wires the organizer export (US-008). */
@Configuration
public class ExportBeans {

  @Bean
  WorkbookWriter workbookWriter() {
    return new ExcelWorkbookWriter();
  }

  @Bean
  ExportRegistrations exportRegistrations(RegistrationReader reader, WorkbookWriter writer) {
    return new ExportRegistrations(reader, writer);
  }
}
