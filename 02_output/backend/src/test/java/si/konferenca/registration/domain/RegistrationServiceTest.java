package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrationServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");

  private final List<Registration> stored = new ArrayList<>();
  private final List<UUID> deleted = new ArrayList<>();
  private final List<Registration> notified = new ArrayList<>();
  private final List<byte[]> notifiedCopies = new ArrayList<>();
  private final List<String> verifiedClients = new ArrayList<>();
  private boolean captchaAccepts = true;
  private RuntimeException storeFailure;
  private RuntimeException copyFailure;

  private final CaptchaVerifier captcha =
      (token, client) -> {
        verifiedClients.add(client);
        return captchaAccepts;
      };

  private final RegistrationStore store =
      new RegistrationStore() {
        @Override
        public void saveAtomically(Registration registration, Runnable beforeCommit) {
          beforeCommit.run();
          if (storeFailure != null) {
            throw storeFailure;
          }
          stored.add(registration);
        }

        @Override
        public List<Registration> findAll() {
          return stored;
        }
      };

  private final JsonCopyStore copies =
      new JsonCopyStore() {
        @Override
        public byte[] write(Registration registration) {
          if (copyFailure != null) {
            throw copyFailure;
          }
          return ("copy-" + registration.id()).getBytes();
        }

        @Override
        public void delete(UUID id) {
          deleted.add(id);
        }
      };

  private final RegistrationNotifier notifier =
      (registration, copy) -> {
        notified.add(registration);
        notifiedCopies.add(copy);
      };

  private final RegistrationService service =
      new RegistrationService(
          new RegistrationValidator(RegistrationValidatorTest.CATALOG),
          captcha,
          store,
          copies,
          notifier,
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void storesThenNotifiesWithTheCopyBytes() {
    Registration r =
        service.register(RegistrationValidatorTest.external(List.of("ws"), List.of("privacy")));

    assertThat(r.registeredAt()).isEqualTo(NOW);
    assertThat(r.consents()).extracting(GivenConsent::givenAt).containsExactly(NOW);
    assertThat(r.options()).extracting(SelectedOption::name).containsExactly("Workshop");
    assertThat(stored).containsExactly(r);
    assertThat(notified).containsExactly(r);
    assertThat(notifiedCopies.get(0)).isEqualTo(("copy-" + r.id()).getBytes());
    assertThat(verifiedClients).containsExactly("127.0.0.1");
    assertThat(deleted).isEmpty();
  }

  @Test
  void rejectedCaptchaStoresNothing() {
    captchaAccepts = false;

    assertThatThrownBy(
            () ->
                service.register(RegistrationValidatorTest.external(List.of(), List.of("privacy"))))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.errors())
                    .containsExactly(
                        new FieldError("captchaToken", RegistrationService.CAPTCHA_REJECTED)));
    assertThat(stored).isEmpty();
    assertThat(notified).isEmpty();
  }

  @Test
  void invalidSubmissionNeverReachesTheCaptcha() {
    assertThatThrownBy(
            () -> service.register(RegistrationValidatorTest.external(List.of(), List.of())))
        .isInstanceOf(RegistrationRejectedException.class);
    assertThat(verifiedClients).isEmpty();
  }

  @Test
  void failedCommitDeletesTheWrittenCopy() {
    storeFailure = new IllegalStateException("commit failed");

    assertThatThrownBy(
            () ->
                service.register(RegistrationValidatorTest.external(List.of(), List.of("privacy"))))
        .isInstanceOf(StorageFailedException.class)
        .hasCause(storeFailure);
    assertThat(deleted).hasSize(1);
    assertThat(notified).isEmpty();
  }

  @Test
  void failedCopyDeletesNothingAndNotifiesNobody() {
    copyFailure = new IllegalStateException("disk full");

    assertThatThrownBy(
            () ->
                service.register(RegistrationValidatorTest.external(List.of(), List.of("privacy"))))
        .isInstanceOf(StorageFailedException.class);
    assertThat(deleted).isEmpty();
    assertThat(notified).isEmpty();
  }

  @Test
  void exportServicePassesAllStoredRegistrationsToTheExporter() {
    Registration r =
        service.register(RegistrationValidatorTest.external(List.of(), List.of("privacy")));
    List<List<Registration>> seen = new ArrayList<>();
    ExportService export =
        new ExportService(
            store,
            list -> {
              seen.add(list);
              return new byte[] {1, 2};
            });

    assertThat(export.exportAll()).containsExactly(1, 2);
    assertThat(seen).containsExactly(List.of(r));
  }

  @Test
  void optionsAreSelectableOnlyWhenActiveAndAvailable() {
    ConferenceOption ext = RegistrationValidatorTest.CATALOG.options().get(1);
    ConferenceOption inactive = RegistrationValidatorTest.CATALOG.options().get(2);

    assertThat(ext.selectableBy(RegistrationType.EXTERNAL)).isTrue();
    assertThat(ext.selectableBy(RegistrationType.STUDENT)).isFalse();
    assertThat(inactive.selectableBy(RegistrationType.EXTERNAL)).isFalse();
    assertThat(RegistrationValidatorTest.CATALOG.activeOptions())
        .extracting(ConferenceOption::id)
        .containsExactly("ws", "ext");
    assertThat(RegistrationValidatorTest.CATALOG.option("old")).contains(inactive);
    assertThat(RegistrationValidatorTest.CATALOG.option("none")).isEmpty();
  }

  @Test
  void labelsAreTheContractTexts() {
    assertThat(RegistrationType.STUDENT.label()).isEqualTo("Student");
    assertThat(OptionCategory.OTHER.label()).isEqualTo("Other activity");
    assertThat(OptionCategory.OTHER.pluralLabel()).isEqualTo("Other activities");
  }
}
