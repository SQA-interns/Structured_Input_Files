package si.konferenca.registration.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import si.konferenca.registration.application.Ports.CaptchaVerifier;
import si.konferenca.registration.application.Ports.RegistrationNotifier;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.Submission;
import si.konferenca.registration.domain.SubmissionValidator;

/** Use case: register a participant (US-001, US-002, US-004 … US-007; spec §4). */
@Service
public class RegisterParticipant {

  public static final String CAPTCHA_REJECTED = "Anti-automation check failed. Please try again.";

  private static final Logger LOG = LoggerFactory.getLogger(RegisterParticipant.class);

  private final ConferenceCatalogue catalogue;
  private final CaptchaVerifier captcha;
  private final RegistrationStorage storage;
  private final RegistrationNotifier notifier;
  private final Clock clock;

  public RegisterParticipant(
      ConferenceCatalogue catalogue,
      CaptchaVerifier captcha,
      RegistrationStorage storage,
      RegistrationNotifier notifier,
      Clock clock) {
    this.catalogue = catalogue;
    this.captcha = captcha;
    this.storage = storage;
    this.notifier = notifier;
    this.clock = clock;
  }

  /** The outcome of a submission. */
  public sealed interface Outcome {}

  /** Stored; the emails were attempted. */
  public record Accepted(Registration registration) implements Outcome {}

  /** Rejected; nothing was stored and no email sent. */
  public record Rejected(List<FieldError> errors) implements Outcome {}

  public Outcome register(Submission submission, String clientIp) {
    Submission valid = submission.normalized();
    List<FieldError> errors = new SubmissionValidator(catalogue).validate(valid);
    if (!errors.isEmpty()) {
      return new Rejected(errors);
    }
    if (!captcha.verify(valid.recaptchaToken(), clientIp)) {
      return new Rejected(List.of(new FieldError("recaptchaToken", CAPTCHA_REJECTED)));
    }
    Registration registration =
        Registration.accept(
            valid, catalogue, UUID.randomUUID(), clock.instant().truncatedTo(ChronoUnit.MILLIS));
    byte[] copy = storage.store(registration);
    LOG.info("Registration {} accepted", registration.id());
    notify(registration, copy);
    return new Accepted(registration);
  }

  /** Emails after the commit; a failure never changes the outcome (OQ-03, ES-07). */
  private void notify(Registration registration, byte[] copy) {
    try {
      notifier.sendConfirmation(registration);
    } catch (RuntimeException e) {
      LOG.warn(
          "Email confirmation for registration {} failed ({})",
          registration.id(),
          e.getClass().getSimpleName());
    }
    try {
      notifier.notifyOrganizers(registration, copy);
    } catch (RuntimeException e) {
      LOG.warn(
          "Email organizer-notification for registration {} failed ({})",
          registration.id(),
          e.getClass().getSimpleName());
    }
  }
}
