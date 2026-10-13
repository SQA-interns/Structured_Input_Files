package si.konferenca.registration.adapter.persistence;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.adapter.persistence.RegistrationEntity.ConsentRow;
import si.konferenca.registration.adapter.persistence.RegistrationEntity.OptionRow;
import si.konferenca.registration.application.Ports.RegistrationStore;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.Registration.GivenConsent;
import si.konferenca.registration.domain.Registration.SelectedOption;
import si.konferenca.registration.domain.RegistrationType;

/** PostgreSQL storage of registrations through JPA (BR-07, AC-005-01). */
@Component
public class JpaRegistrationStore implements RegistrationStore {

  private final RegistrationRepository repository;

  public JpaRegistrationStore(RegistrationRepository repository) {
    this.repository = repository;
  }

  /** Inserts and flushes, so a database error surfaces before the JSON copy is written. */
  @Override
  public void save(Registration r) {
    repository.saveAndFlush(
        new RegistrationEntity(
            r.id(),
            r.type().name(),
            r.firstName(),
            r.lastName(),
            r.email(),
            r.organization(),
            r.studyInstitution(),
            r.studyProgramme(),
            r.studentId(),
            r.registeredAt(),
            r.options().stream()
                .map(o -> new OptionRow(o.id(), o.name(), o.category().name()))
                .toList(),
            r.consents().stream()
                .map(c -> new ConsentRow(c.id(), c.text(), c.givenAt()))
                .toList()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Registration> findAllOldestFirst() {
    return repository.findAll(Sort.by("registeredAt", "id")).stream()
        .map(JpaRegistrationStore::toDomain)
        .toList();
  }

  private static Registration toDomain(RegistrationEntity e) {
    return new Registration(
        e.id(),
        RegistrationType.valueOf(e.type()),
        e.registeredAt(),
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
                    new SelectedOption(
                        o.optionId(), o.optionName(), OptionCategory.valueOf(o.category())))
            .toList(),
        e.consents().stream()
            .map(c -> new GivenConsent(c.consentId(), c.consentText(), c.givenAt()))
            .toList());
  }
}
