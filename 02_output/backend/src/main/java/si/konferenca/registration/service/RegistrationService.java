package si.konferenca.registration.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.FieldErrors;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationValidator;

/** The registration use case: validate, verify, store, then notify (spec section 5). */
@Service
public class RegistrationService {

  public static final String CAPTCHA_FAILED = "Confirm that you are not a robot.";

  private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final RegistrationStorage storage;
  private final RegistrationNotifier notifier;
  private final Clock clock;

  public RegistrationService(
      ConferenceCatalog catalog,
      CaptchaVerifier captcha,
      RegistrationStorage storage,
      RegistrationNotifier notifier,
      Clock clock) {
    this.validator = new RegistrationValidator(catalog);
    this.captcha = captcha;
    this.storage = storage;
    this.notifier = notifier;
    this.clock = clock;
  }

  /**
   * Accepts the submission or throws {@link RegistrationRejectedException}. Emails are sent only
   * after the row and the copy exist; an email failure does not change the outcome (OQ-03).
   */
  public Registration register(
      RegistrationSubmission submission, String recaptchaToken, String clientIp) {
    RegistrationValidator.Result result = validator.validate(submission);
    FieldErrors errors = new FieldErrors();
    errors.addAll(result.errors());
    if (recaptchaToken == null || recaptchaToken.isBlank()) {
      errors.add("recaptchaToken", CAPTCHA_FAILED);
    } else if (!captcha.verify(recaptchaToken, clientIp)) {
      errors.add("recaptchaToken", CAPTCHA_FAILED);
    }
    if (!errors.isEmpty()) {
      throw new RegistrationRejectedException(errors);
    }
    Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
    Registration registration = result.registration().accept(UUID.randomUUID(), now);
    byte[] jsonCopy = storage.store(registration);
    send("participant confirmation", registration, () -> notifier.notifyParticipant(registration));
    send(
        "organizer notification",
        registration,
        () -> notifier.notifyOrganizers(registration, jsonCopy));
    return registration;
  }

  private static void send(String kind, Registration registration, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      LOG.warn("Email {} failed for registration {}", kind, registration.id());
    }
  }
}
