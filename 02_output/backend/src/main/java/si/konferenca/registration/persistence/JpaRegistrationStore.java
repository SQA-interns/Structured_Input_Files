package si.konferenca.registration.persistence;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.DuplicateEmailException;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.GivenConsent;
import si.konferenca.registration.domain.AcceptedRegistration.Participant;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;

/** PostgreSQL adapter of {@link RegistrationStore} (US-005, US-008). */
@Component
class JpaRegistrationStore implements RegistrationStore {

  private static final String UNIQUE_EMAIL = "uq_registration_email_normalized";

  private final RegistrationRepository repository;

  JpaRegistrationStore(RegistrationRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean existsByNormalizedEmail(String normalizedEmail) {
    return repository.existsByEmailNormalized(normalizedEmail);
  }

  @Override
  public void insert(AcceptedRegistration registration) {
    Participant p = registration.participant();
    RegistrationEntity entity =
        new RegistrationEntity(
            registration.reference(),
            registration.type(),
            p.firstName(),
            p.lastName(),
            p.email(),
            registration.normalizedEmail(),
            p.organization(),
            p.studyInstitution(),
            p.studyProgramme(),
            p.studentId(),
            registration.submittedAt());
    for (SelectedOption option : registration.options()) {
      entity
          .options()
          .add(new RegistrationEntity.OptionRow(option.id(), option.name(), option.category()));
    }
    for (GivenConsent consent : registration.consents()) {
      entity
          .consents()
          .add(new RegistrationEntity.ConsentRow(consent.id(), consent.text(), consent.givenAt()));
    }
    try {
      repository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException e) {
      if (mentionsUniqueEmail(e)) {
        throw new DuplicateEmailException();
      }
      throw e;
    }
  }

  @Override
  public List<AcceptedRegistration> findAllOrderedBySubmission() {
    return repository.findAllByOrderBySubmittedAtAscIdAsc().stream()
        .map(JpaRegistrationStore::toDomain)
        .toList();
  }

  private static boolean mentionsUniqueEmail(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t.getMessage() != null && t.getMessage().contains(UNIQUE_EMAIL)) {
        return true;
      }
    }
    return false;
  }

  private static AcceptedRegistration toDomain(RegistrationEntity e) {
    return new AcceptedRegistration(
        e.reference(),
        e.submittedAt(),
        e.type(),
        new Participant(
            e.firstName(),
            e.lastName(),
            e.email(),
            e.organization(),
            e.studyInstitution(),
            e.studyProgramme(),
            e.studentId()),
        e.options().stream()
            .map(o -> new SelectedOption(o.optionId(), o.optionName(), o.category()))
            .toList(),
        e.consents().stream()
            .map(c -> new GivenConsent(c.consentId(), c.consentText(), c.givenAt()))
            .toList());
  }
}
