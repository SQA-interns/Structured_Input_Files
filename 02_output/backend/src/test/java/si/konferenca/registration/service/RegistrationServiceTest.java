package si.konferenca.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.persistence.RegistrationStore;

class RegistrationServiceTest {

  static final ConferenceCatalog CATALOG =
      new ConferenceCatalog(
          List.of(
              new ConferenceOption(
                  "ws", "W", OptionCategory.WORKSHOP, true, EnumSet.allOf(RegistrationType.class))),
          List.of(new ConsentDefinition("privacy", "P", true)));
  static final RegistrationSubmission VALID =
      new RegistrationSubmission(
          "EXTERNAL",
          "Ana",
          "Novak",
          "a@b.co",
          "Org",
          null,
          null,
          null,
          List.of("ws"),
          List.of("privacy"));

  private final CaptchaVerifier captcha = mock(CaptchaVerifier.class);
  private final RegistrationStorage storage = mock(RegistrationStorage.class);
  private final RegistrationNotifier notifier = mock(RegistrationNotifier.class);
  private final Clock clock =
      Clock.fixed(Instant.parse("2026-10-13T08:00:00.123456789Z"), ZoneOffset.UTC);
  private final RegistrationService service =
      new RegistrationService(CATALOG, captcha, storage, notifier, clock);

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void acceptedRegistrationIsStoredThenParticipantThenOrganizersAreNotified() {
    when(captcha.verify("tok", "1.2.3.4")).thenReturn(true);
    byte[] copy = {1, 2, 3};
    when(storage.store(any())).thenReturn(copy);

    Registration r = service.register(VALID, "tok", "1.2.3.4");

    assertThat(r.registeredAt()).isEqualTo(Instant.parse("2026-10-13T08:00:00.123Z"));
    assertThat(r.id()).isNotNull();
    assertThat(r.firstName()).isEqualTo("Ana");
    InOrder order = inOrder(storage, notifier);
    order.verify(storage).store(r);
    order.verify(notifier).notifyParticipant(r);
    order.verify(notifier).notifyOrganizers(r, copy);
  }

  @Test
  void emailFailuresDoNotChangeTheOutcome() {
    when(captcha.verify(any(), any())).thenReturn(true);
    when(storage.store(any())).thenReturn(new byte[0]);
    doThrow(new IllegalStateException("smtp")).when(notifier).notifyParticipant(any());
    doThrow(new IllegalStateException("smtp")).when(notifier).notifyOrganizers(any(), any());

    Registration r = service.register(VALID, "tok", null);

    assertThat(r).isNotNull();
    verify(notifier).notifyOrganizers(any(), any());
  }

  @Test
  void missingOrBlankTokenIsRejectedWithoutAskingTheVerifier() {
    for (String token : new String[] {null, "", "  "}) {
      assertThatThrownBy(() -> service.register(VALID, token, "1.2.3.4"))
          .isInstanceOfSatisfying(
              RegistrationRejectedException.class,
              e ->
                  assertThat(e.fieldErrors())
                      .containsExactlyEntriesOf(
                          java.util.Map.of("recaptchaToken", RegistrationService.CAPTCHA_FAILED)));
    }
    verifyNoInteractions(captcha, storage, notifier);
  }

  @Test
  void tokenRejectedByVerifierIsRejectedAndNothingStored() {
    when(captcha.verify("bad", "1.2.3.4")).thenReturn(false);

    assertThatThrownBy(() -> service.register(VALID, "bad", "1.2.3.4"))
        .isInstanceOf(RegistrationRejectedException.class)
        .hasMessage("Registration rejected");
    verifyNoInteractions(storage, notifier);
  }

  @Test
  void fieldErrorsAndCaptchaErrorsAreReportedTogether() {
    when(captcha.verify(any(), any())).thenReturn(false);
    RegistrationSubmission invalid =
        new RegistrationSubmission(
            "EXTERNAL", "", "Novak", "a@b.co", "Org", null, null, null, null, List.of("privacy"));

    assertThatThrownBy(() -> service.register(invalid, "tok", null))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e -> assertThat(e.fieldErrors()).containsOnlyKeys("firstName", "recaptchaToken"));
    verify(storage, never()).store(any());
  }

  @Test
  void clockBeanIsUtc() {
    assertThat(new ServiceConfiguration().clock().getZone()).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  void storageWritesRowThenCopyAndRemovesTheCopyUnlessCommitted() {
    RegistrationStore store = mock(RegistrationStore.class);
    JsonCopyStore copies = mock(JsonCopyStore.class);
    Registration r = mock(Registration.class);
    UUID id = UUID.randomUUID();
    when(r.id()).thenReturn(id);
    when(copies.write(r)).thenReturn(new byte[] {7});
    TransactionSynchronizationManager.initSynchronization();

    byte[] written = new RegistrationStorage(store, copies).store(r);

    assertThat(written).containsExactly(7);
    InOrder order = inOrder(store, copies);
    order.verify(store).insert(r);
    order.verify(copies).write(r);
    List<TransactionSynchronization> synchronizations =
        TransactionSynchronizationManager.getSynchronizations();
    assertThat(synchronizations).hasSize(1);
    synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    verify(copies, never()).delete(any());
    synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(copies).delete(id);
    synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    ArgumentCaptor<UUID> deleted = ArgumentCaptor.forClass(UUID.class);
    verify(copies, org.mockito.Mockito.times(2)).delete(deleted.capture());
    assertThat(deleted.getAllValues()).containsOnly(id);
  }
}
