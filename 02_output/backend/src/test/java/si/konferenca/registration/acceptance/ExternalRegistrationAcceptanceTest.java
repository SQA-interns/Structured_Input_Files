package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.CaptchaMock;
import si.konferenca.registration.acceptance.support.TestEnvironment;
import tools.jackson.databind.JsonNode;

/** US-001 External participant registration, through the REST API. */
class ExternalRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_01_formConfigOffersExternalOptionsAndConsents() {
    HttpResponse<String> response = get("/api/form-config");

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode config = json(response);
    List<String> externalOptions =
        config
            .path("options")
            .valueStream()
            .filter(
                o ->
                    o.path("audience").valueStream().anyMatch(a -> "EXTERNAL".equals(a.asString())))
            .map(o -> o.path("id").asString())
            .toList();
    assertThat(externalOptions)
        .containsExactlyInAnyOrder("ws-ai", "ev-gala", "meal-lunch", "other-tour");
    assertThat(config.path("consents").valueStream().map(c -> c.path("id").asString()))
        .containsExactlyInAnyOrder("data-processing", "photos");
  }

  @Test
  void ac_001_02_validExternalRegistrationIsAcceptedAndExported() {
    String email = uniqueEmail("ac-001-02");

    JsonNode accepted = registerAccepted(validExternal(email));

    String reference = accepted.path("reference").asString();
    Map<String, String> row = exportRow(exportRows(), reference);
    assertThat(row)
        .containsEntry("Type", "EXTERNAL")
        .containsEntry("First name", "Ana")
        .containsEntry("Last name", "Novak")
        .containsEntry("Email", email)
        .containsEntry("Organization / institution", "Institut Jožef Stefan")
        .containsEntry("Workshops", "Delavnica: umetna inteligenca")
        .containsEntry("Meals", "Kosilo");
    assertThat(row.get("Consents")).contains("data-processing");
  }

  static Stream<Arguments> missingRequiredFields() {
    return Stream.of("firstName", "lastName", "email", "organization")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, null),
                    Arguments.of(field, ""),
                    Arguments.of(field, "   ")));
  }

  @ParameterizedTest(name = "AC-001-03 {0} = [{1}]")
  @MethodSource("missingRequiredFields")
  void ac_001_03_missingOrBlankRequiredFieldIsRejected(String field, String value) {
    String email = uniqueEmail("ac-001-03");
    Map<String, Object> body = validExternal(email);
    if (value == null) {
      body.remove(field);
    } else {
      body.put(field, value);
    }
    long before = registrationCount();
    long copiesBefore = jsonCopyCount(TestEnvironment.JSON_DIR);

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, field, "REQUIRED");
    assertThat(json(response).path("error").asString()).isEqualTo("VALIDATION_FAILED");
    assertThat(registrationCount()).isEqualTo(before);
    assertThat(jsonCopyCount(TestEnvironment.JSON_DIR)).isEqualTo(copiesBefore);
  }

  @Test
  void ac_001_04_surroundingWhitespaceIsNotStored() {
    String email = uniqueEmail("ac-001-04");
    Map<String, Object> body = validExternal("  " + email + "  ");
    body.put("firstName", "  Ana ");
    body.put("lastName", " Novak  ".replace(' ', ' '));
    body.put("organization", "  Institut Jožef Stefan\t".replace('\t', ' '));

    JsonNode accepted = registerAccepted(body);

    Map<String, String> row = registrationRow(accepted.path("reference").asString());
    assertThat(row)
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", email)
        .containsEntry("organization", "Institut Jožef Stefan");
  }

  @ParameterizedTest(name = "AC-001-05 invalid email [{0}]")
  @MethodSource("invalidEmails")
  void ac_001_05_invalidEmailIsRejected(String email) {
    long before = registrationCount();

    HttpResponse<String> response = register(validExternal(email));

    assertFieldError(response, 400, "email", "INVALID_EMAIL");
    assertThat(registrationCount()).isEqualTo(before);
  }

  static Stream<String> invalidEmails() {
    return Stream.of(
        "ana.novak",
        "ana@",
        "@example.si",
        "ana@example",
        "ana novak@example.si",
        "ana@@example.si",
        "ana@example..si",
        ".ana@example.si");
  }

  @Test
  void ac_001_06_slovenianCharactersSurviveEveryStore() {
    String email = uniqueEmail("ac-001-06");
    Map<String, Object> body = validExternal(email);
    body.put("firstName", "Čedomir Šime");
    body.put("lastName", "Žagar-Čuk");
    body.put("organization", "Fakulteta za računalništvo in informatiko, Ljubljana – ŠŽČ");

    JsonNode accepted = registerAccepted(body);
    String reference = accepted.path("reference").asString();

    assertThat(registrationRow(reference))
        .containsEntry("first_name", "Čedomir Šime")
        .containsEntry("last_name", "Žagar-Čuk")
        .containsEntry(
            "organization", "Fakulteta za računalništvo in informatiko, Ljubljana – ŠŽČ");
    JsonNode copy = JSON.readTree(readString(jsonCopy(TestEnvironment.JSON_DIR, reference)));
    assertThat(copy.path("participant").path("firstName").asString()).isEqualTo("Čedomir Šime");
    assertThat(copy.path("participant").path("lastName").asString()).isEqualTo("Žagar-Čuk");
    JsonNode participantMail = MAILPIT.awaitSingleMessageTo(email, m -> true);
    assertThat(participantMail.path("Text").asString()).contains("Čedomir Šime", "Žagar-Čuk");
    JsonNode organizerMail =
        MAILPIT.awaitSingleMessageTo(
            TestEnvironment.ORGANIZER_ONE, m -> m.path("Subject").asString().contains(reference));
    assertThat(organizerMail.path("Text").asString())
        .contains(
            "Čedomir Šime",
            "Žagar-Čuk",
            "Fakulteta za računalništvo in informatiko, Ljubljana – ŠŽČ");
    assertThat(exportRow(exportRows(), reference))
        .containsEntry("First name", "Čedomir Šime")
        .containsEntry("Last name", "Žagar-Čuk")
        .containsEntry(
            "Organization / institution",
            "Fakulteta za računalništvo in informatiko, Ljubljana – ŠŽČ");
  }

  static Stream<Arguments> invalidOptionSelections() {
    return Stream.of(
        Arguments.of(List.of("ws-unknown"), "UNKNOWN_OPTION"),
        Arguments.of(List.of("ws-ai", "ws-old"), "OPTION_NOT_AVAILABLE"),
        Arguments.of(List.of("ws-ai", "ws-ai"), "DUPLICATE_OPTION"));
  }

  @ParameterizedTest(name = "AC-001-07 options {0} -> {1}")
  @MethodSource("invalidOptionSelections")
  void ac_001_07_unknownInactiveOrRepeatedOptionIsRejected(List<String> optionIds, String code) {
    String email = uniqueEmail("ac-001-07");
    Map<String, Object> body = validExternal(email);
    body.put("optionIds", optionIds);

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, "optionIds", code);
    assertThat(registrationCountByEmail(email)).isZero();
  }

  @Test
  void ac_001_08_missingMandatoryConsentIsRejected() {
    String email = uniqueEmail("ac-001-08");
    Map<String, Object> body = validExternal(email);
    body.put("consentIds", List.of("photos"));

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, "consents.data-processing", "CONSENT_REQUIRED");
    assertThat(registrationCountByEmail(email)).isZero();
  }

  @Test
  void ac_001_09_missingCaptchaTokenIsRejected() {
    String email = uniqueEmail("ac-001-09-missing");
    Map<String, Object> body = validExternal(email);
    body.remove("captchaToken");

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, "captchaToken", "REQUIRED");
    assertThat(registrationCountByEmail(email)).isZero();
  }

  @Test
  void ac_001_09_tokenRejectedByVerificationServiceIsRejected() {
    String email = uniqueEmail("ac-001-09-rejected");
    Map<String, Object> body = validExternal(email);
    body.put("captchaToken", CaptchaMock.REJECTED_TOKEN);

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, "captchaToken", "CAPTCHA_FAILED");
    assertThat(json(response).path("error").asString()).isEqualTo("CAPTCHA_FAILED");
    assertThat(TestEnvironment.CAPTCHA.requests())
        .anySatisfy(r -> assertThat(r).containsEntry("response", CaptchaMock.REJECTED_TOKEN));
    assertThat(registrationCountByEmail(email)).isZero();
    MAILPIT.assertNoMessageTo(email, Duration.ofSeconds(2));
  }

  @Test
  void ac_001_09_tokenAcceptedByVerificationServiceLetsRegistrationThrough() {
    String email = uniqueEmail("ac-001-09-accepted");

    registerAccepted(validExternal(email));

    assertThat(TestEnvironment.CAPTCHA.requests())
        .anySatisfy(
            r ->
                assertThat(r)
                    .containsEntry("secret", CaptchaMock.SECRET)
                    .containsEntry("response", CaptchaMock.VALID_TOKEN));
    assertThat(registrationCountByEmail(email)).isEqualTo(1);
  }

  @Test
  void ac_001_10_secondRegistrationWithSameEmailIsRejected() {
    String email = uniqueEmail("ac-001-10");
    registerAccepted(validExternal(email.toLowerCase()));

    HttpResponse<String> response = register(validStudent(email.toUpperCase()));

    assertFieldError(response, 409, "email", "DUPLICATE_EMAIL");
    assertThat(json(response).path("error").asString()).isEqualTo("DUPLICATE_EMAIL");
    assertThat(registrationCountByEmail(email)).isEqualTo(1);
  }

  static Stream<Arguments> tooLongOrControlCharacters() {
    String longLocal = "a".repeat(64);
    String longEmail =
        longLocal + "@" + "b".repeat(60) + "." + "c".repeat(60) + "." + "d".repeat(60) + ".si";
    return Stream.of(
        Arguments.of("firstName", "A".repeat(201), "TOO_LONG"),
        Arguments.of("lastName", "Ž".repeat(201), "TOO_LONG"),
        Arguments.of("organization", "O".repeat(201), "TOO_LONG"),
        Arguments.of("email", longEmail + "x".repeat(255 - longEmail.length()), "TOO_LONG"),
        Arguments.of("firstName", "Ana\r\nBcc: victim@example.si", "INVALID_CHARACTERS"),
        Arguments.of("lastName", "Novak\nX", "INVALID_CHARACTERS"),
        Arguments.of("organization", "IJS\u0007", "INVALID_CHARACTERS"));
  }

  @ParameterizedTest(name = "AC-001-11 {0} -> {2}")
  @MethodSource("tooLongOrControlCharacters")
  void ac_001_11_tooLongOrControlCharacterIsRejected(String field, String value, String code) {
    String email = uniqueEmail("ac-001-11");
    Map<String, Object> body = validExternal(email);
    body.put(field, value);
    long before = registrationCount();

    HttpResponse<String> response = register(body);

    assertFieldError(response, 400, field, code);
    assertThat(registrationCount()).isEqualTo(before);
  }

  @Test
  void ac_001_11_valuesAtTheLengthLimitAreAccepted() {
    String email = uniqueEmail("ac-001-11-limit");
    Map<String, Object> body = validExternal(email);
    body.put("firstName", "A".repeat(200));
    body.put("organization", "Č".repeat(200));

    registerAccepted(body);

    assertThat(registrationCountByEmail(email)).isEqualTo(1);
  }
}
