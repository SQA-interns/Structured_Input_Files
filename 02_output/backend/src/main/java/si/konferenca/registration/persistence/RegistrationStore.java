package si.konferenca.registration.persistence;

import java.util.List;
import org.springframework.stereotype.Component;
import si.konferenca.registration.domain.Registration;

/** Stores and reads registrations, mapping between the domain and the table rows. */
@Component
public class RegistrationStore {

  private final RegistrationRepository repository;

  public RegistrationStore(RegistrationRepository repository) {
    this.repository = repository;
  }

  /** Inserts the registration and flushes, so a database error surfaces here. */
  public void insert(Registration r) {
    repository.saveAndFlush(
        new RegistrationEntity(
            r.id(),
            r.type(),
            r.firstName(),
            r.lastName(),
            r.email(),
            r.organization(),
            r.studyInstitution(),
            r.studyProgramme(),
            r.studentId(),
            r.registeredAt(),
            r.options().stream()
                .map(o -> new RegistrationEntity.OptionRow(o.id(), o.name(), o.category()))
                .toList(),
            r.consents().stream()
                .map(c -> new RegistrationEntity.ConsentRow(c.id(), c.text(), c.givenAt()))
                .toList()));
  }

  /** Every registration in order of registration time; call inside a transaction. */
  public List<Registration> findAllInOrder() {
    return repository.findAllByOrderByRegisteredAtAsc().stream()
        .map(RegistrationStore::toDomain)
        .toList();
  }

  private static Registration toDomain(RegistrationEntity e) {
    return new Registration(
        e.id(),
        e.registeredAt(),
        e.type(),
        e.firstName(),
        e.lastName(),
        e.email(),
        e.organization(),
        e.studyInstitution(),
        e.studyProgramme(),
        e.studentId(),
        e.options().stream()
            .map(
                o ->
                    new Registration.SelectedOption(
                        o.optionId(), o.optionName(), o.optionCategory()))
            .toList(),
        e.consents().stream()
            .map(c -> new Registration.GivenConsent(c.consentId(), c.consentText(), c.givenAt()))
            .toList());
  }
}
