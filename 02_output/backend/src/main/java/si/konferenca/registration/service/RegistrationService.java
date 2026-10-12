package si.konferenca.registration.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import si.konferenca.registration.conference.ConferenceOption;
import si.konferenca.registration.conference.ConsentDefinition;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;
import si.konferenca.registration.infra.MailNotifier;
import si.konferenca.registration.infra.RecaptchaVerifier;
import si.konferenca.registration.service.RegistrationStore.StoredRegistration;

/** The registration use case of specification 4.2: validate, verify, store, notify. */
@Service
public class RegistrationService {

  static final String RECAPTCHA_REJECTED = "The reCAPTCHA check failed. Please try again.";

  private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

  private final RegistrationValidator validator;
  private final RecaptchaVerifier recaptcha;
  private final RegistrationStore store;
  private final MailNotifier mail;

  public RegistrationService(
      RegistrationValidator validator,
      RecaptchaVerifier recaptcha,
      RegistrationStore store,
      MailNotifier mail) {
    this.validator = validator;
    this.recaptcha = recaptcha;
    this.store = store;
    this.mail = mail;
  }

  /** Accepts a registration or throws {@link RegistrationRejectedException}. */
  public Registration register(RegistrationCommand command) {
    ValidatedRegistration valid = validator.validate(command);
    if (!recaptcha.verify(command.recaptchaToken().strip(), command.clientIp())) {
      throw new RegistrationRejectedException(
          List.of(new FieldError("recaptchaToken", RECAPTCHA_REJECTED)));
    }
    // The database keeps microseconds; the JSON copy uses the same value.
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    Registration registration =
        new Registration(UUID.randomUUID(), valid.type(), valid.participant(), now);
    for (ConferenceOption option : valid.options()) {
      registration.addOption(
          new SelectedOption(option.id(), option.name(), option.category().name()));
    }
    for (ConsentDefinition consent : valid.consents()) {
      registration.addConsent(new GivenConsent(consent.id(), consent.text(), now));
    }
    StoredRegistration stored = store.save(registration);
    notify(stored);
    return stored.registration();
  }

  /** Emails only after the row and the copy exist; failures never undo the registration. */
  private void notify(StoredRegistration stored) {
    UUID id = stored.registration().getId();
    try {
      mail.sendParticipantConfirmation(stored.registration());
    } catch (RuntimeException e) {
      LOG.warn("Email participant-confirmation failed for registration {}", id);
    }
    try {
      int failed = mail.sendOrganizerNotifications(stored.registration(), stored.jsonCopy());
      if (failed > 0) {
        LOG.warn("Email organizer-notification failed {} time(s) for registration {}", failed, id);
      }
    } catch (RuntimeException e) {
      LOG.warn("Email organizer-notification failed for registration {}", id);
    }
  }
}
