package si.konferenca.registration.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.GivenConsent;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;
import si.konferenca.registration.domain.RegistrationForm;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.ValidRegistration;

/**
 * Registration use case (US-001, US-002): validate, verify reCAPTCHA, reject duplicates, store
 * (database and JSON copy) and only then notify (BR-06, BR-07, D-10).
 */
public class RegistrationService {

  private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final RegistrationStore store;
  private final RegistrationRecorder recorder;
  private final Clock clock;
  private final Notifier notifier;

  public RegistrationService(
      RegistrationValidator validator,
      CaptchaVerifier captcha,
      RegistrationStore store,
      RegistrationRecorder recorder,
      Clock clock,
      Notifier notifier) {
    this.validator = validator;
    this.captcha = captcha;
    this.store = store;
    this.recorder = recorder;
    this.clock = clock;
    this.notifier = notifier;
  }

  /**
   * Accepts the registration once it is stored.
   *
   * @throws si.konferenca.registration.domain.ValidationException if a field is invalid
   * @throws CaptchaRejectedException if the reCAPTCHA token is not accepted
   * @throws DuplicateEmailException if the email is already registered
   */
  public AcceptedRegistration register(RegistrationForm form, String clientIp) {
    ValidRegistration valid = validator.validate(form);
    if (!captcha.verify(form.captchaToken().strip(), clientIp)) {
      throw new CaptchaRejectedException();
    }
    if (store.existsByNormalizedEmail(
        AcceptedRegistration.normalizeEmail(valid.participant().email()))) {
      throw new DuplicateEmailException();
    }
    Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
    AcceptedRegistration accepted =
        new AcceptedRegistration(
            UUID.randomUUID(),
            now,
            valid.type(),
            valid.participant(),
            valid.options().stream()
                .map(o -> new SelectedOption(o.id(), o.name(), o.category()))
                .toList(),
            valid.consents().stream().map(c -> new GivenConsent(c.id(), c.text(), now)).toList());
    recorder.record(accepted);
    notifyParticipant(accepted);
    return accepted;
  }

  /** A failed email never undoes the stored registration (D-10); logged without personal data. */
  private void notifyParticipant(AcceptedRegistration accepted) {
    try {
      notifier.sendParticipantConfirmation(accepted);
    } catch (RuntimeException e) {
      LOG.warn(
          "Participant confirmation for registration {} failed: {}",
          accepted.reference(),
          e.getClass().getSimpleName());
    }
  }
}
