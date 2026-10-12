package si.konferenca.registration.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import si.konferenca.registration.conference.ConferenceOption;
import si.konferenca.registration.conference.ConsentDefinition;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;
import si.konferenca.registration.infra.RecaptchaVerifier;

/** The registration use case of specification 4.2: validate, verify, store. */
@Service
public class RegistrationService {

  static final String RECAPTCHA_REJECTED = "The reCAPTCHA check failed. Please try again.";

  private final RegistrationValidator validator;
  private final RecaptchaVerifier recaptcha;
  private final RegistrationStore store;

  public RegistrationService(
      RegistrationValidator validator, RecaptchaVerifier recaptcha, RegistrationStore store) {
    this.validator = validator;
    this.recaptcha = recaptcha;
    this.store = store;
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
    return store.save(registration).registration();
  }
}
