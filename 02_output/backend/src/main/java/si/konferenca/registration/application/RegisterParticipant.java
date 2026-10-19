package si.konferenca.registration.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.ValidatedRegistration;
import si.konferenca.registration.domain.ValidationResult;

/** Use case: validate, verify the captcha, store (US-001, US-002, US-005). */
public class RegisterParticipant {

  private final RegistrationValidator validator;
  private final CaptchaVerifier captchaVerifier;
  private final RegistrationStore store;
  private final Clock clock;

  public RegisterParticipant(
      ConferenceConfiguration configuration,
      CaptchaVerifier captchaVerifier,
      RegistrationStore store,
      JsonCopyStore jsonCopyStore,
      Clock clock) {
    this.validator = new RegistrationValidator(configuration);
    this.captchaVerifier = captchaVerifier;
    this.store = store;
    this.clock = clock;
  }

  public Registration register(RegistrationSubmission submission) {
    ValidationResult result = validator.validate(submission);
    if (!result.isValid()) {
      throw new RegistrationRejectedException(result.errors());
    }
    ValidatedRegistration valid = result.registration();
    if (!captchaVerifier.verify(valid.recaptchaToken())) {
      throw new RegistrationRejectedException(
          List.of(new FieldError("recaptchaToken", "The anti-automation check failed.")));
    }
    Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
    Registration registration =
        new Registration(
            UUID.randomUUID(),
            valid.type(),
            now,
            valid.firstName(),
            valid.lastName(),
            valid.email(),
            valid.organization(),
            valid.studyInstitution(),
            valid.studyProgramme(),
            valid.studentId(),
            valid.options(),
            valid.consentIds().stream().map(id -> new GivenConsent(id, now)).toList());
    store.save(registration);
    return registration;
  }
}
