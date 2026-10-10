package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.Catalogs;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.RegistrationForm;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.ValidationException;

class RegistrationServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-11T08:00:00.123456Z");

  private final CaptchaVerifier captcha = mock(CaptchaVerifier.class);
  private final RegistrationStore store = mock(RegistrationStore.class);
  private final RegistrationRecorder recorder = mock(RegistrationRecorder.class);
  private final Notifier notifier = mock(Notifier.class);
  private final RegistrationService service =
      new RegistrationService(
          new RegistrationValidator(Catalogs.sample()),
          captcha,
          store,
          recorder,
          Clock.fixed(NOW, ZoneOffset.UTC),
          notifier);

  private static RegistrationForm form(String email) {
    return new RegistrationForm(
        "EXTERNAL",
        "Ana",
        "Novak",
        email,
        "IJS",
        null,
        null,
        null,
        List.of("ws-ai", "meal-lunch"),
        List.of("photos", "data"),
        " tok ");
  }

  @Test
  void storesThenNotifiesWithTheStoredCopy() {
    when(captcha.verify("tok", "1.2.3.4")).thenReturn(true);
    byte[] copy = {1, 2};
    when(recorder.record(any())).thenReturn(copy);

    AcceptedRegistration accepted = service.register(form("Ana@Example.si"), "1.2.3.4");

    assertThat(accepted.submittedAt()).isEqualTo(Instant.parse("2026-10-11T08:00:00.123Z"));
    assertThat(accepted.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(accepted.options())
        .extracting(AcceptedRegistration.SelectedOption::id)
        .containsExactly("ws-ai", "meal-lunch");
    assertThat(accepted.consents())
        .extracting(AcceptedRegistration.GivenConsent::id)
        .containsExactly("data", "photos");
    assertThat(accepted.consents())
        .allSatisfy(c -> assertThat(c.givenAt()).isEqualTo(accepted.submittedAt()));
    InOrder order = inOrder(store, recorder, notifier);
    order.verify(store).existsByNormalizedEmail("ana@example.si");
    order.verify(recorder).record(accepted);
    order.verify(notifier).sendParticipantConfirmation(accepted);
    order.verify(notifier).sendOrganizerNotification(accepted, copy);
  }

  @Test
  void invalidFormStopsBeforeCaptcha() {
    assertThatThrownBy(() -> service.register(form("bad"), "ip"))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(captcha, store, recorder, notifier);
  }

  @Test
  void rejectedCaptchaStopsBeforeStorage() {
    when(captcha.verify(anyString(), any())).thenReturn(false);

    assertThatThrownBy(() -> service.register(form("a@example.si"), "ip"))
        .isInstanceOf(CaptchaRejectedException.class);
    verifyNoInteractions(store, recorder, notifier);
  }

  @Test
  void duplicateEmailStopsBeforeStorage() {
    when(captcha.verify(anyString(), any())).thenReturn(true);
    when(store.existsByNormalizedEmail("a@example.si")).thenReturn(true);

    assertThatThrownBy(() -> service.register(form("a@example.si"), "ip"))
        .isInstanceOf(DuplicateEmailException.class);
    verify(recorder, never()).record(any());
    verifyNoInteractions(notifier);
  }

  @Test
  void failedStorageSendsNoEmail() {
    when(captcha.verify(anyString(), any())).thenReturn(true);
    when(recorder.record(any())).thenThrow(new IllegalStateException("db"));

    assertThatThrownBy(() -> service.register(form("a@example.si"), "ip"))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(notifier);
  }

  @Test
  void failedEmailsDoNotUndoTheRegistration() {
    when(captcha.verify(anyString(), any())).thenReturn(true);
    when(recorder.record(any())).thenReturn(new byte[0]);
    doThrow(new IllegalStateException("smtp")).when(notifier).sendParticipantConfirmation(any());
    doThrow(new IllegalStateException("smtp"))
        .when(notifier)
        .sendOrganizerNotification(any(), any());

    AcceptedRegistration accepted = service.register(form("a@example.si"), "ip");

    assertThat(accepted).isNotNull();
    verify(notifier).sendOrganizerNotification(any(), any());
  }

  @Test
  void formConfigShowsActiveOptions() {
    OptionCatalog catalog = Catalogs.sample();
    FormConfigService.FormConfig config =
        new FormConfigService(catalog, new FormConfigService.FormSettings("Konf", "site", true))
            .formConfig();

    assertThat(config.conferenceName()).isEqualTo("Konf");
    assertThat(config.options()).isEqualTo(catalog.activeOptions());
    assertThat(config.consents()).isEqualTo(catalog.consents());
    assertThat(config.recaptchaSiteKey()).isEqualTo("site");
    assertThat(config.recaptchaTestMode()).isTrue();
  }

  @Test
  void exportWritesAllStoredRegistrations() {
    WorkbookWriter writer = mock(WorkbookWriter.class);
    List<AcceptedRegistration> all = List.of();
    when(store.findAllOrderedBySubmission()).thenReturn(all);
    when(writer.write(all)).thenReturn(new byte[] {7});

    assertThat(new ExportService(store, writer).exportWorkbook()).containsExactly(7);
  }

  @Test
  void recorderInsertsThenWritesTheCopy() {
    JsonCopyStore copies = mock(JsonCopyStore.class);
    RegistrationStore registrations = mock(RegistrationStore.class);
    AcceptedRegistration r = mock(AcceptedRegistration.class);
    when(copies.write(r)).thenReturn(new byte[] {9});
    org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
    try {
      byte[] copy = new RegistrationRecorder(registrations, copies).record(r);

      assertThat(copy).containsExactly(9);
      InOrder order = inOrder(registrations, copies);
      order.verify(registrations).insert(r);
      order.verify(copies).write(r);
      var sync =
          org.springframework.transaction.support.TransactionSynchronizationManager
              .getSynchronizations();
      assertThat(sync).hasSize(1);
      sync.get(0)
          .afterCompletion(
              org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
      verify(copies, never()).delete(any());
      sync.get(0)
          .afterCompletion(
              org.springframework.transaction.support.TransactionSynchronization
                  .STATUS_ROLLED_BACK);
      ArgumentCaptor<java.util.UUID> deleted = ArgumentCaptor.forClass(java.util.UUID.class);
      verify(copies).delete(deleted.capture());
    } finally {
      org.springframework.transaction.support.TransactionSynchronizationManager
          .clearSynchronization();
    }
  }
}
