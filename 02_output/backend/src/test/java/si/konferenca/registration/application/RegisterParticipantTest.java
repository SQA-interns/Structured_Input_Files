package si.konferenca.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationType;

class RegisterParticipantTest {

  static final ConferenceConfiguration CONFIG =
      ConferenceConfiguration.of(
          "Conf",
          List.of(new ConsentDefinition("privacy", "I agree.", true)),
          List.of(new ConferenceOption("ws-a", "Workshop A", OptionCategory.WORKSHOP, true, null)));

  static final Instant NOW = Instant.parse("2026-10-19T12:00:00.123456Z");

  final List<Registration> stored = new ArrayList<>();
  final List<String> events = new ArrayList<>();
  RuntimeException copyFailure = null;
  final List<String> verifiedTokens = new ArrayList<>();
  boolean captchaAccepts = true;

  final RegisterParticipant useCase =
      new RegisterParticipant(
          CONFIG,
          token -> {
            verifiedTokens.add(token);
            return captchaAccepts;
          },
          registration -> {
            events.add("save");
            stored.add(registration);
          },
          registration -> {
            events.add("copy");
            if (copyFailure != null) {
              throw copyFailure;
            }
          },
          registration -> events.add("notify"),
          Clock.fixed(NOW, ZoneOffset.UTC));

  static RegistrationSubmission submission(String firstName, String token) {
    return new RegistrationSubmission(
        "EXTERNAL",
        firstName,
        " Novak ",
        "ana@example.com",
        "IJS",
        null,
        null,
        null,
        List.of("ws-a"),
        List.of("privacy"),
        token);
  }

  @Test
  void AC_001_01_acceptedRegistrationIsStoredWithIdTimeAndConsentTimes() {
    Registration registration = useCase.register(submission(" Ana", "token-1"));

    assertThat(stored).containsExactly(registration);
    assertThat(registration.id()).isNotNull();
    assertThat(registration.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(registration.registeredAt()).isEqualTo(Instant.parse("2026-10-19T12:00:00.123Z"));
    assertThat(registration.firstName()).isEqualTo("Ana");
    assertThat(registration.lastName()).isEqualTo("Novak");
    assertThat(registration.organization()).isEqualTo("IJS");
    assertThat(registration.options()).extracting(ConferenceOption::id).containsExactly("ws-a");
    assertThat(registration.consents())
        .containsExactly(new GivenConsent("privacy", registration.registeredAt()));
    assertThat(verifiedTokens).containsExactly("token-1");
  }

  @Test
  void AC_001_01_eachRegistrationGetsItsOwnId() {
    Registration first = useCase.register(submission("Ana", "t"));
    Registration second = useCase.register(submission("Ana", "t"));

    assertThat(first.id()).isNotEqualTo(second.id());
  }

  @Test
  void AC_001_02_invalidFieldsAreRejectedWithoutCaptchaCallOrStorage() {
    assertThatThrownBy(() -> useCase.register(submission(" ", "token")))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.fieldErrors())
                    .extracting(FieldError::field)
                    .containsExactly("firstName"));
    assertThat(verifiedTokens).isEmpty();
    assertThat(stored).isEmpty();
  }

  @Test
  void AC_001_07_tokenRejectedByVerifierIsAFieldErrorAndNothingIsStored() {
    captchaAccepts = false;

    assertThatThrownBy(() -> useCase.register(submission("Ana", "forged")))
        .isInstanceOfSatisfying(
            RegistrationRejectedException.class,
            e ->
                assertThat(e.fieldErrors())
                    .extracting(FieldError::field)
                    .containsExactly("recaptchaToken"));
    assertThat(verifiedTokens).containsExactly("forged");
    assertThat(stored).isEmpty();
  }

  @Test
  void AC_005_02_jsonCopyIsWrittenAfterTheDatabaseRow() {
    useCase.register(submission("Ana", "t"));

    assertThat(events).containsExactly("save", "copy", "notify");
  }

  @Test
  void AC_005_03_jsonCopyFailureFailsTheRegistration() {
    copyFailure = new IllegalStateException("disk full");

    assertThatThrownBy(() -> useCase.register(submission("Ana", "t"))).isSameAs(copyFailure);
    assertThat(events).containsExactly("save", "copy");
  }

  @Test
  void AC_005_02_rejectedSubmissionWritesNoCopy() {
    captchaAccepts = false;

    assertThatThrownBy(() -> useCase.register(submission("Ana", "t")))
        .isInstanceOf(RegistrationRejectedException.class);
    assertThat(events).isEmpty();
  }

  @Test
  void AC_006_03_notificationFollowsStorageAndIsSkippedWhenStorageFails() {
    useCase.register(submission("Ana", "t"));
    assertThat(events).containsExactly("save", "copy", "notify");

    events.clear();
    copyFailure = new IllegalStateException("disk full");
    assertThatThrownBy(() -> useCase.register(submission("Ana", "t")));
    assertThat(events).containsExactly("save", "copy");
  }
}
