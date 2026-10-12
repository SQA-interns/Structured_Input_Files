package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;
import si.konferenca.registration.domain.DuplicateEmailException;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationForm;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.ValidationException;

class RegistrationServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-12T10:00:00.123456Z");

  /** Records commits and rollbacks; can fail the commit. */
  static final class RecordingTransactions extends AbstractPlatformTransactionManager {
    final List<String> events = new ArrayList<>();
    boolean failCommit;

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
      events.add("begin");
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
      if (failCommit) {
        throw new org.springframework.transaction.TransactionSystemException("commit failed");
      }
      events.add("commit");
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
      events.add("rollback");
    }
  }

  final List<String> calls = new ArrayList<>();
  final List<Registration> saved = new ArrayList<>();
  boolean emailTaken;
  boolean failCopy;
  boolean captchaAnswer = true;
  final List<String> captchaTokens = new ArrayList<>();
  final RecordingTransactions transactions = new RecordingTransactions();

  final RegistrationStore store =
      new RegistrationStore() {
        @Override
        public boolean emailExists(String normalizedEmail) {
          calls.add("exists:" + normalizedEmail);
          return emailTaken;
        }

        @Override
        public void save(Registration registration) {
          calls.add("save");
          saved.add(registration);
        }

        @Override
        public List<Registration> findAllBySubmissionTime() {
          return saved;
        }
      };

  final RegistrationCopyWriter copies =
      new RegistrationCopyWriter() {
        @Override
        public byte[] write(Registration registration) {
          calls.add("write");
          if (failCopy) {
            throw new IllegalStateException("disk full");
          }
          return new byte[] {1, 2, 3};
        }

        @Override
        public void delete(UUID registrationId) {
          calls.add("delete");
        }
      };

  final Notifier notifier = (registration, copy) -> calls.add("notify:" + copy.length);

  RegistrationService service() {
    OptionCatalog catalog =
        new OptionCatalog(
            new Consent("c", "t"),
            List.of(
                new ConferenceOption(
                    "ws", "W", Category.WORKSHOP, true, EnumSet.allOf(RegistrationType.class))));
    CaptchaVerifier captcha =
        (token, ip) -> {
          captchaTokens.add(token + "@" + ip);
          return captchaAnswer;
        };
    return new RegistrationService(
        new RegistrationValidator(catalog),
        captcha,
        store,
        copies,
        notifier,
        new TransactionTemplate(transactions),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  static RegistrationForm valid() {
    return new RegistrationForm(
        "EXTERNAL", "Ana", "Novak", "Ana@X.si", "Acme", null, null, null, List.of("ws"), true);
  }

  @Test
  void storesCopiesCommitsThenNotifies() {
    RegistrationService.Accepted accepted = service().register(valid(), " tok ", "10.0.0.1");

    assertThat(calls).containsExactly("exists:ana@x.si", "save", "write", "notify:3");
    assertThat(transactions.events).containsExactly("begin", "commit");
    assertThat(captchaTokens).containsExactly("tok@10.0.0.1");
    assertThat(accepted.jsonCopy()).containsExactly(1, 2, 3);
    assertThat(accepted.registration().submittedAt())
        .isEqualTo(Instant.parse("2026-10-12T10:00:00.123Z"));
  }

  @Test
  void acceptedCopiesItsBytes() {
    byte[] bytes = {1};
    RegistrationService.Accepted accepted = new RegistrationService.Accepted(null, bytes);
    bytes[0] = 9;
    accepted.jsonCopy()[0] = 7;

    assertThat(accepted.jsonCopy()).containsExactly(1);
  }

  @Test
  void invalidFormIsRejectedBeforeTheCaptchaAndStorage() {
    RegistrationForm invalid =
        new RegistrationForm("EXTERNAL", "", "N", "a@b.si", "A", null, null, null, List.of(), true);

    assertThatThrownBy(() -> service().register(invalid, "tok", "ip"))
        .isInstanceOf(ValidationException.class);
    assertThat(captchaTokens).isEmpty();
    assertThat(calls).isEmpty();
  }

  @Test
  void missingBlankOrRejectedTokenIsAFieldError() {
    for (String token : new String[] {null, "  "}) {
      assertThatThrownBy(() -> service().register(valid(), token, "ip"))
          .isInstanceOfSatisfying(
              ValidationException.class,
              e ->
                  assertThat(e.errors())
                      .containsExactly(
                          new FieldError("recaptchaToken", RegistrationService.CAPTCHA_FAILED)));
    }
    assertThat(captchaTokens).isEmpty();
    captchaAnswer = false;

    assertThatThrownBy(() -> service().register(valid(), "tok", "ip"))
        .isInstanceOf(ValidationException.class);
    assertThat(calls).isEmpty();
  }

  @Test
  void duplicateEmailRollsBackWithoutWritingOrNotifying() {
    emailTaken = true;

    assertThatThrownBy(() -> service().register(valid(), "tok", "ip"))
        .isInstanceOf(DuplicateEmailException.class);
    assertThat(calls).containsExactly("exists:ana@x.si", "delete");
    assertThat(transactions.events).containsExactly("begin", "rollback");
  }

  @Test
  void copyFailureRollsBackAndDoesNotNotify() {
    failCopy = true;

    assertThatThrownBy(() -> service().register(valid(), "tok", "ip")).hasMessage("disk full");
    assertThat(calls).containsExactly("exists:ana@x.si", "save", "write", "delete");
    assertThat(transactions.events).containsExactly("begin", "rollback");
  }

  @Test
  void commitFailureDeletesTheCopyAndDoesNotNotify() {
    transactions.failCommit = true;

    assertThatThrownBy(() -> service().register(valid(), "tok", "ip"))
        .hasMessageContaining("commit failed");
    assertThat(calls).containsExactly("exists:ana@x.si", "save", "write", "delete");
  }

  @Test
  void exportWritesAllStoredRegistrations() {
    service().register(valid(), "tok", "ip");
    List<List<Registration>> written = new ArrayList<>();
    ExportService export =
        new ExportService(
            store,
            registrations -> {
              written.add(registrations);
              return new byte[] {42};
            });

    assertThat(export.export()).containsExactly(42);
    assertThat(written).containsExactly(saved);
  }
}
