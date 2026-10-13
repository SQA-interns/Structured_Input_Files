package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.TestRegistrations;
import si.konferenca.registration.application.Ports.CaptchaVerifier;
import si.konferenca.registration.application.Ports.JsonCopyStore;
import si.konferenca.registration.application.Ports.RegistrationNotifier;
import si.konferenca.registration.application.Ports.RegistrationStore;
import si.konferenca.registration.application.Ports.WorkbookWriter;
import si.konferenca.registration.application.RegisterParticipant.Accepted;
import si.konferenca.registration.application.RegisterParticipant.Rejected;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.Submission;

/** Spec §4: order of validation, token check, storage and emails. */
class RegisterParticipantTest {

  private static final ConferenceCatalogue CATALOGUE =
      new ConferenceCatalogue(
          List.of(
              new ConferenceOption(
                  "ws",
                  "Workshop",
                  OptionCategory.WORKSHOP,
                  true,
                  EnumSet.allOf(RegistrationType.class))),
          List.of(new ConsentDefinition("privacy", "I agree", true)));

  private final CaptchaVerifier captcha = mock(CaptchaVerifier.class);
  private final RegistrationStorage storage = mock(RegistrationStorage.class);
  private final RegistrationNotifier notifier = mock(RegistrationNotifier.class);
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-10-13T20:15:03.123456Z"), ZoneOffset.UTC);
  private final RegisterParticipant useCase =
      new RegisterParticipant(CATALOGUE, captcha, storage, notifier, clock);

  private static Submission valid() {
    return new Submission(
        "EXTERNAL",
        " Ana ",
        "Novak",
        "ana@example.si",
        "Org",
        null,
        null,
        null,
        List.of("ws"),
        Map.of("privacy", true),
        "token");
  }

  @Test
  void invalidSubmissionIsRejectedWithoutTokenCheckOrStorage() {
    Submission invalid =
        new Submission(
            "EXTERNAL",
            "",
            "Novak",
            "ana@example.si",
            "Org",
            null,
            null,
            null,
            List.of(),
            Map.of("privacy", true),
            "token");

    var outcome = useCase.register(invalid, "1.2.3.4");

    assertThat(outcome).isInstanceOf(Rejected.class);
    assertThat(((Rejected) outcome).errors())
        .containsExactly(new FieldError("firstName", "This field is required."));
    verify(captcha, never()).verify(any(), any());
    verify(storage, never()).store(any());
  }

  @Test
  void rejectedTokenIsAFieldErrorAndStoresNothing() {
    when(captcha.verify("token", "1.2.3.4")).thenReturn(false);

    var outcome = useCase.register(valid(), "1.2.3.4");

    assertThat(((Rejected) outcome).errors())
        .containsExactly(new FieldError("recaptchaToken", RegisterParticipant.CAPTCHA_REJECTED));
    verify(storage, never()).store(any());
    verify(notifier, never()).sendConfirmation(any());
  }

  @Test
  void acceptedRegistrationIsStoredThenMailed() {
    when(captcha.verify("token", "1.2.3.4")).thenReturn(true);
    byte[] copy = {1, 2};
    when(storage.store(any())).thenReturn(copy);

    var outcome = useCase.register(valid(), "1.2.3.4");

    Registration r = ((Accepted) outcome).registration();
    assertThat(r.firstName()).isEqualTo("Ana");
    assertThat(r.registeredAt()).isEqualTo(Instant.parse("2026-10-13T20:15:03.123Z"));
    verify(storage).store(r);
    verify(notifier).sendConfirmation(r);
    verify(notifier).notifyOrganizers(r, copy);
  }

  @Test
  void mailFailuresDoNotChangeTheOutcome() {
    when(captcha.verify(any(), any())).thenReturn(true);
    when(storage.store(any())).thenReturn(new byte[0]);
    doThrow(new IllegalStateException("smtp down")).when(notifier).sendConfirmation(any());
    doThrow(new IllegalStateException("smtp down")).when(notifier).notifyOrganizers(any(), any());

    assertThat(useCase.register(valid(), null)).isInstanceOf(Accepted.class);
    verify(notifier).notifyOrganizers(any(), any());
  }

  @Test
  void storageWritesRowThenCopyAndDeletesTheCopyUnlessCommitted() {
    RegistrationStore store = mock(RegistrationStore.class);
    JsonCopyStore copies = mock(JsonCopyStore.class);
    Registration r = TestRegistrations.external();
    when(copies.write(r)).thenReturn(new byte[] {7});
    TransactionSynchronizationManager.initSynchronization();
    try {
      assertThat(new RegistrationStorage(store, copies).store(r)).containsExactly(7);
      verify(store).save(r);
      List<TransactionSynchronization> callbacks =
          TransactionSynchronizationManager.getSynchronizations();
      assertThat(callbacks).hasSize(1);

      callbacks.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
      verify(copies, never()).delete(any());
      callbacks.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
      verify(copies).delete(r.id());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void exportWritesEveryStoredRegistration() {
    RegistrationStore store = mock(RegistrationStore.class);
    WorkbookWriter writer = mock(WorkbookWriter.class);
    List<Registration> all = List.of(TestRegistrations.external());
    when(store.findAllOldestFirst()).thenReturn(all);
    when(writer.write(all)).thenReturn(new byte[] {9});

    assertThat(new ExportRegistrations(store, writer).export()).containsExactly(9);
  }

  @Test
  void formConfigurationHidesTheSiteKeyInTestMode() {
    var real = new GetFormConfiguration(CATALOGUE, new FormSettings("C", false, "site")).get();
    assertThat(real.siteKey()).isEqualTo("site");
    assertThat(real.captchaTestMode()).isFalse();
    assertThat(real.activeOptions()).hasSize(1);
    var test = new GetFormConfiguration(CATALOGUE, new FormSettings("C", true, "site")).get();
    assertThat(test.siteKey()).isNull();
    assertThat(test.conferenceName()).isEqualTo("C");
    assertThat(test.consents()).hasSize(1);
  }

  @Test
  void storageFailureCarriesItsCause() {
    var cause = new java.io.IOException("x");
    var failure = new StorageFailedException("m", cause);
    assertThat(failure).hasMessage("m").hasCause(cause);
  }
}
