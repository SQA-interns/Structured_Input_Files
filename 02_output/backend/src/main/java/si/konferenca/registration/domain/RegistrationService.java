package si.konferenca.registration.domain;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Use case: accept a registration (US-001, US-002, US-005; specification section 5). */
public class RegistrationService {

  static final String CAPTCHA_REJECTED = "The anti-robot check failed. Please try again.";

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final RegistrationStore store;
  private final JsonCopyStore jsonCopies;
  private final RegistrationNotifier notifier;
  private final Clock clock;

  public RegistrationService(
      RegistrationValidator validator,
      CaptchaVerifier captcha,
      RegistrationStore store,
      JsonCopyStore jsonCopies,
      RegistrationNotifier notifier,
      Clock clock) {
    this.validator = validator;
    this.captcha = captcha;
    this.store = store;
    this.jsonCopies = jsonCopies;
    this.notifier = notifier;
    this.clock = clock;
  }

  /** Validates, verifies the captcha and stores; returns the accepted registration. */
  public Registration register(RegistrationSubmission submission) {
    ValidSubmission valid = validator.validate(submission);
    if (!captcha.verify(valid.captchaToken(), submission.clientAddress())) {
      throw new RegistrationRejectedException(
          List.of(new FieldError("captchaToken", CAPTCHA_REJECTED)));
    }
    Registration registration = valid.accept(UUID.randomUUID(), clock.instant());
    AtomicReference<byte[]> copy = new AtomicReference<>();
    try {
      store.saveAtomically(registration, () -> copy.set(jsonCopies.write(registration)));
    } catch (RuntimeException e) {
      if (copy.get() != null) {
        jsonCopies.delete(registration.id());
      }
      throw new StorageFailedException(e);
    }
    notifier.registrationAccepted(registration, copy.get());
    return registration;
  }
}
