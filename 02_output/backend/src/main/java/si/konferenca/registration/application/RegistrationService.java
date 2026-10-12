package si.konferenca.registration.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;
import si.konferenca.registration.domain.DuplicateEmailException;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationForm;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.ValidationException;

/** Use case: accept a registration (US-001, US-002, US-005; specification S4, S6). */
public class RegistrationService {

  static final String CAPTCHA_FAILED = "Please confirm you are not a robot.";

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final RegistrationStore store;
  private final RegistrationCopyWriter copies;
  private final TransactionTemplate transactions;
  private final Clock clock;

  public RegistrationService(
      RegistrationValidator validator,
      CaptchaVerifier captcha,
      RegistrationStore store,
      RegistrationCopyWriter copies,
      TransactionTemplate transactions,
      Clock clock) {
    this.validator = validator;
    this.captcha = captcha;
    this.store = store;
    this.copies = copies;
    this.transactions = transactions;
    this.clock = clock;
  }

  /**
   * Validates, verifies the token, then stores the row and the JSON copy in one transaction. The
   * result is returned only when both are written (AR-05).
   *
   * @throws ValidationException for invalid fields or a rejected token
   * @throws DuplicateEmailException if the email is already registered
   */
  public Accepted register(RegistrationForm form, String captchaToken, String clientIp) {
    Registration registration =
        validator.validate(form, UUID.randomUUID(), clock.instant().truncatedTo(ChronoUnit.MILLIS));
    if (captchaToken == null
        || captchaToken.isBlank()
        || !captcha.verify(captchaToken.strip(), clientIp)) {
      throw new ValidationException(List.of(new FieldError("recaptchaToken", CAPTCHA_FAILED)));
    }
    byte[] copy = null;
    try {
      copy =
          Objects.requireNonNull(
              transactions.execute(
                  status -> {
                    if (store.emailExists(registration.normalizedEmail())) {
                      throw new DuplicateEmailException();
                    }
                    store.save(registration);
                    return copies.write(registration);
                  }),
              "JSON copy");
    } finally {
      if (copy == null) {
        copies.delete(registration.id());
      }
    }
    return new Accepted(registration, copy);
  }

  /** An accepted registration and the bytes of its JSON copy. */
  public record Accepted(Registration registration, byte[] jsonCopy) {

    public Accepted {
      jsonCopy = jsonCopy.clone();
    }

    @Override
    public byte[] jsonCopy() {
      return jsonCopy.clone();
    }
  }
}
