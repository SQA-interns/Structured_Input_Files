package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;
import static si.konferenca.registration.acceptance.support.Payloads.with;
import static si.konferenca.registration.acceptance.support.Payloads.without;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Api;
import si.konferenca.registration.acceptance.support.Payloads;

/** US-001 External participant registration. */
class ExternalRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-001-01: a valid external submission is accepted with a registration id")
  void ac_001_01_validExternalSubmissionIsAccepted() {
    Map<String, Object> submission = external();

    Api.Response response = api.register(submission);

    assertThat(response.status()).as(response.text()).isEqualTo(201);
    String id = response.json().read("$.id");
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(db.registration(UUID.fromString(id))).isPresent();
  }

  static Stream<Arguments> missingRequiredFields() {
    return Payloads.EXTERNAL_REQUIRED.stream()
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, ""),
                    Arguments.of(field, "   ")));
  }

  @ParameterizedTest(name = "AC-001-02: {0} = [{1}] is rejected")
  @MethodSource("missingRequiredFields")
  void ac_001_02_missingOrBlankRequiredFieldIsRejected(String field, String value) {
    Map<String, Object> valid = external();
    Map<String, Object> submission =
        "missing".equals(value) ? without(valid, field) : with(valid, field, value);

    Api.Response response = api.register(submission);

    assertRejected(response, field, JSON_COPY_DIR, valid);
  }

  @Test
  @DisplayName("AC-001-03: leading and trailing spaces are removed before storing")
  void ac_001_03_surroundingWhitespaceIsTrimmed() {
    Map<String, Object> valid = external();
    Map<String, Object> submission = with(valid, "firstName", "  Ana  ");
    submission = with(submission, "lastName", "\t" + valid.get("lastName") + "  ");
    submission = with(submission, "email", "  " + valid.get("email") + " ");
    submission = with(submission, "organization", "  Institut Jožef Stefan ");

    UUID id = registerAccepted(submission);

    Map<String, Object> row = db.registration(id).orElseThrow();
    assertThat(row.get("first_name")).isEqualTo("Ana");
    assertThat(row.get("last_name")).isEqualTo(valid.get("lastName"));
    assertThat(row.get("email")).isEqualTo(valid.get("email"));
    assertThat(row.get("organization")).isEqualTo("Institut Jožef Stefan");
  }

  @ParameterizedTest(name = "AC-001-04: email [{0}] is rejected")
  @ValueSource(
      strings = {
        "ana.novak",
        "ana@",
        "@example.test",
        "ana novak@example.test",
        "ana@example",
        "ana@@example.test",
        "ana@example.test\r\nBcc: attacker@evil.test"
      })
  void ac_001_04_invalidEmailIsRejected(String email) {
    Map<String, Object> valid = external();
    Map<String, Object> submission = with(valid, "email", email);

    Api.Response response = api.register(submission);

    assertRejected(response, "email", JSON_COPY_DIR, valid);
    assertThat(db.countByEmail(email)).isZero();
  }

  static Stream<Arguments> invalidOptionSelections() {
    return Stream.of(
        Arguments.of("unknown", List.of("ws-ai", "no-such-option")),
        Arguments.of("inactive", List.of("ws-old")),
        Arguments.of("duplicate", List.of("ws-ai", "ws-ai")));
  }

  @ParameterizedTest(name = "AC-001-05: {0} option is rejected")
  @MethodSource("invalidOptionSelections")
  void ac_001_05_unknownInactiveOrDuplicateOptionIsRejected(String kind, List<String> optionIds) {
    Map<String, Object> valid = external();
    Map<String, Object> submission = with(valid, "optionIds", optionIds);

    Api.Response response = api.register(submission);

    assertRejected(response, "optionIds", JSON_COPY_DIR, valid);
  }

  static Stream<Arguments> missingMandatoryConsent() {
    return Stream.of(
        Arguments.of("no consent", List.of()), Arguments.of("optional only", List.of("photo")));
  }

  @ParameterizedTest(name = "AC-001-06: {0} is rejected")
  @MethodSource("missingMandatoryConsent")
  void ac_001_06_missingMandatoryConsentIsRejected(String kind, List<String> consentIds) {
    Map<String, Object> valid = external();
    Map<String, Object> submission = with(valid, "consentIds", consentIds);

    Api.Response response = api.register(submission);

    assertRejected(response, "consents.privacy", JSON_COPY_DIR, valid);
  }

  @ParameterizedTest(name = "AC-001-07: captcha token [{0}] is rejected")
  @ValueSource(strings = {"missing", "", "TEST-FAIL"})
  void ac_001_07_missingOrRejectedCaptchaIsRejected(String token) {
    Map<String, Object> valid = external();
    Map<String, Object> submission =
        "missing".equals(token)
            ? without(valid, "captchaToken")
            : with(valid, "captchaToken", token);

    Api.Response response = api.register(submission);

    assertRejected(response, "captchaToken", JSON_COPY_DIR, valid);
  }
}
