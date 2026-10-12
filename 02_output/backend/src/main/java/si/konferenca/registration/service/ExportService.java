package si.konferenca.registration.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.domain.RegistrationRepository;
import si.konferenca.registration.infra.ExcelWriter;

/** The organizer export (US-008, BR-08): all registrations in registration order. */
@Service
public class ExportService {

  private final RegistrationRepository repository;
  private final ExcelWriter excel;

  public ExportService(RegistrationRepository repository, ExcelWriter excel) {
    this.repository = repository;
    this.excel = excel;
  }

  @Transactional(readOnly = true)
  public byte[] exportWorkbook() {
    return excel.write(repository.findAllByOrderByRegisteredAtAsc());
  }
}
