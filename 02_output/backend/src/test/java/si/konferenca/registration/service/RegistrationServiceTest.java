package si.konferenca.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import si.konferenca.registration.conference.ConferenceConfiguration;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;
import si.konferenca.registration.infra.MailNotifier;
import si.konferenca.registration.infra.RecaptchaVerifier;
import si.konferenca.registration.service.RegistrationStore.StoredRegistration;

class RegistrationServiceTest {

  private final RecaptchaVerifier recaptcha = mock(RecaptchaVerifier.class);
  private final RegistrationStore store = mock(RegistrationStore.class);
  private final MailNotifier mail = mock(MailNotifier.class);
  private final byte[] copy = {1, 2, 3};
  private RegistrationService service;

  @BeforeEach
  void setUp() {
    RegistrationValidator validator =
        new RegistrationValidator(
            new ConferenceConfiguration(
                List.of(RegistrationValidatorTest.WORKSHOP, RegistrationValidatorTest.DINNER),
                List.of(RegistrationValidatorTest.PRIVACY, RegistrationValidatorTest.PHOTO)));
    service = new RegistrationService(validator, recaptcha, store, mail);
    when(store.save(any())).thenAnswer(i -> new StoredRegistration(i.getArgument(0), copy));
  }

  @Test
  void storesAVerifiedRegistrationThenNotifies() {
    when(recaptcha.verify("token", "1.2.3.4")).thenReturn(true);
    Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

    Registration registration =
        service.register(RegistrationValidatorTest.external("ws", "dinner"));

    assertThat(registration.getId()).isNotNull();
    assertThat(registration.getFirstName()).isEqualTo("Ana");
    assertThat(registration.getRegisteredAt())
        .isBetween(before, Instant.now())
        .isEqualTo(registration.getRegisteredAt().truncatedTo(ChronoUnit.MICROS));
    assertThat(registration.getOptions())
        .containsExactly(
            new SelectedOption("ws", "Workshop", "WORKSHOP"),
            new SelectedOption("dinner", "Dinner", "MEAL"));
    assertThat(registration.getConsents())
        .containsExactly(new GivenConsent("privacy", "Privacy", registration.getRegisteredAt()));
    verify(store).save(registration);
    verify(mail).sendParticipantConfirmation(registration);
    verify(mail).sendOrganizerNotifications(registration, copy);
  }

  @Test
  void stripsTheTokenBeforeVerifying() {
    RegistrationCommand command =
        new RegistrationCommand(
            "EXTERNAL",
            "A",
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            List.of(),
            List.of("privacy"),
            " token ",
            "ip");
    when(recaptcha.verify("token", "ip")).thenReturn(true);

    service.register(command);

    verify(recaptcha).verify("token", "ip");
  }

  @Test
  void rejectsAFailedRecaptchaWithoutStoringOrSending() {
    when(recaptcha.verify(any(), any())).thenReturn(false);

    assertThatThrownBy(() -> service.register(RegistrationValidatorTest.external("ws")))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.fieldErrors())
                    .containsExactly(
                        new FieldError("recaptchaToken", RegistrationService.RECAPTCHA_REJECTED)));
    verifyNoInteractions(store, mail);
  }

  @Test
  void rejectsInvalidFieldsBeforeCallingTheVerifier() {
    assertThatThrownBy(() -> service.register(RegistrationValidatorTest.external("unknown")))
        .isInstanceOf(RegistrationRejectedException.class);
    verifyNoInteractions(recaptcha, store, mail);
  }

  @Test
  void mailFailuresDoNotUndoTheRegistration() {
    when(recaptcha.verify(any(), any())).thenReturn(true);
    org.mockito.Mockito.doThrow(new MailSendException("down"))
        .when(mail)
        .sendParticipantConfirmation(any());
    when(mail.sendOrganizerNotifications(any(), any())).thenThrow(new MailSendException("down"));

    Registration registration = service.register(RegistrationValidatorTest.external());

    assertThat(registration).isNotNull();
    verify(mail).sendOrganizerNotifications(eq(registration), eq(copy));
  }

  @Test
  void partialOrganizerFailuresAreTolerated() {
    when(recaptcha.verify(any(), any())).thenReturn(true);
    when(mail.sendOrganizerNotifications(any(), any())).thenReturn(1);

    assertThat(service.register(RegistrationValidatorTest.external())).isNotNull();
  }

  @Test
  void storeFailuresPropagateAndNothingIsSent() {
    when(recaptcha.verify(any(), any())).thenReturn(true);
    when(store.save(any())).thenThrow(new java.io.UncheckedIOException(new java.io.IOException()));

    assertThatThrownBy(() -> service.register(RegistrationValidatorTest.external()))
        .isInstanceOf(java.io.UncheckedIOException.class);
    verify(mail, never()).sendParticipantConfirmation(any());
    ArgumentCaptor<Registration> unused = ArgumentCaptor.forClass(Registration.class);
    verify(mail, never()).sendOrganizerNotifications(unused.capture(), any());
  }
}
