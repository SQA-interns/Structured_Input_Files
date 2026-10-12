package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.acceptance.support.DefaultContextAcceptanceTest;
import si.konferenca.registration.acceptance.support.Payloads;

/** US-001 External participant registration (AC-001-01 … AC-001-07). */
class ExternalRegistrationAcceptanceTest extends DefaultContextAcceptanceTest {

  @Test
  void AC_001_01_validExternalSubmissionIsAcceptedWithNewId() {
    String email = Payloads.uniqueEmail("ac00101");

    HttpResponse<String> response = postRegistration(Payloads.external(email));

    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    String id = json(response.body()).path("id").asString();
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(
            DB.queryForObject(
                "SELECT count(*) FROM registration WHERE id = ?::uuid", Integer.class, id))
        .isEqualTo(1);
  }

  static Stream<Arguments> requiredFieldsWithBadValues() {
    return Stream.of("firstName", "lastName", "email", "organization")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, ""),
                    Arguments.of(field, "   ")));
  }

  @ParameterizedTest(name = "{0} = [{1}]")
  @MethodSource("requiredFieldsWithBadValues")
  void AC_001_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String value) {
    String email = Payloads.uniqueEmail("ac00102");
    Map<String, Object> body = Payloads.external(email);
    if ("missing".equals(value)) {
      body.remove(field);
    } else {
      body.put(field, value);
    }

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, field);
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_001_03_leadingAndTrailingSpacesAreRemovedBeforeStorage() {
    String email = Payloads.uniqueEmail("ac00103");
    Map<String, Object> body = Payloads.external("  " + email + "  ");
    body.put("firstName", "  Ana ");
    body.put("lastName", "\tNovak  ");
    body.put("organization", "  Acme Testing Ltd   ");

    String id = registerOk(body);

    Map<String, Object> row =
        DB.queryForMap(
            "SELECT first_name, last_name, email, organization FROM registration WHERE id = ?::uuid",
            id);
    assertThat(row)
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", email)
        .containsEntry("organization", "Acme Testing Ltd");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "plainaddress",
        "ana@",
        "@example.com",
        "ana@example",
        "ana novak@example.com",
        "ana@@example.com"
      })
  void AC_001_04_invalidEmailFormatIsRejected(String invalidEmail) {
    String marker = Payloads.uniqueEmail("ac00104");
    Map<String, Object> body = Payloads.external(invalidEmail);
    body.put("organization", marker);

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "email");
    assertThat(
            DB.queryForObject(
                "SELECT count(*) FROM registration WHERE organization = ?", Integer.class, marker))
        .isZero();
    assertThat(copiesMentioning(marker)).isEmpty();
    assertThat(MAIL.countMentioning(marker)).isZero();
  }

  static Stream<Arguments> invalidOptionSelections() {
    return Stream.of(
        Arguments.of("unknown", List.of("ws-does-not-exist")),
        Arguments.of("inactive", List.of("ws-legacy")),
        Arguments.of("listed twice", List.of("ev-opening", "ev-opening")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidOptionSelections")
  void AC_001_05_unknownInactiveOrDuplicateOptionIsRejected(String reason, List<String> optionIds) {
    String email = Payloads.uniqueEmail("ac00105");
    Map<String, Object> body = Payloads.external(email);
    body.put("optionIds", optionIds);

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "optionIds");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_001_06_missingMandatoryConsentIsRejected() {
    String email = Payloads.uniqueEmail("ac00106");
    Map<String, Object> body = Payloads.external(email);
    body.put("consents", List.of("photos"));

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "consents.privacy");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_001_06_absentConsentListIsRejected() {
    String email = Payloads.uniqueEmail("ac00106b");
    Map<String, Object> body = Payloads.external(email);
    body.remove("consents");

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "consents.privacy");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_001_07_missingRecaptchaTokenIsRejectedAndNothingStored() {
    String email = Payloads.uniqueEmail("ac00107");
    Map<String, Object> body = Payloads.external(email);
    body.remove("recaptchaToken");

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "recaptchaToken");
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_001_07_rejectedRecaptchaTokenIsRejectedAndNothingStored() {
    String email = Payloads.uniqueEmail("ac00107b");
    Map<String, Object> body = Payloads.external(email);
    body.put("recaptchaToken", "token-the-verifier-rejects");

    HttpResponse<String> response = postRegistration(body);

    assertRejectedWithFieldError(response, "recaptchaToken");
    assertNothingStoredOrSent(email);
  }
}
