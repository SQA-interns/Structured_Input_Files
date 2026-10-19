package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;

/** US-001: external participant registration through POST /api/registrations. */
class ExternalRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void AC_001_01_validExternalSubmissionIsAcceptedWithNewId() throws Exception {
    MvcResult result = submit(Submissions.validExternal());

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    String id = json(result).path("id").asString();
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(registrationCount()).isEqualTo(1);
  }

  static Stream<Arguments> requiredFieldVariants() {
    return Stream.of("firstName", "lastName", "email", "organization")
        .flatMap(
            field ->
                Stream.of(
                    Arguments.of(field, "missing"),
                    Arguments.of(field, ""),
                    Arguments.of(field, "   \t ")));
  }

  @ParameterizedTest(name = "AC-001-02 {0} = [{1}]")
  @MethodSource("requiredFieldVariants")
  void AC_001_02_missingEmptyOrBlankRequiredFieldIsRejected(String field, String value)
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String email = (String) body.get("email");
    if ("missing".equals(value)) {
      body.remove(field);
    } else {
      body.put(field, value);
    }

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains(field);
    assertNothingStoredOrSent(email);
  }

  @Test
  void AC_001_03_leadingAndTrailingSpacesAreRemovedBeforeStorage() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String email = (String) body.get("email");
    body.put("firstName", "  Ana ");
    body.put("lastName", "\tNovak  ");
    body.put("email", "  " + email + " ");
    body.put("organization", "  Institut Jozef Stefan   ");

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT first_name, last_name, email, organization FROM registration WHERE id = ?",
            UUID.fromString(json(result).path("id").asString()));
    assertThat(row)
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", email)
        .containsEntry("organization", "Institut Jozef Stefan");
  }

  @ParameterizedTest(name = "AC-001-04 email = [{0}]")
  @ValueSource(
      strings = {
        "ana.novak",
        "ana.novak@",
        "@example.com",
        "ana novak@example.com",
        "ana@@example.com",
        "ana@exa mple.com"
      })
  void AC_001_04_invalidEmailFormatIsRejected(String invalidEmail) throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("email", invalidEmail);

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains("email");
    assertThat(registrationCount()).isZero();
    assertThat(jsonCopies()).isEmpty();
    assertThat(mailpit.messagesToAfterGracePeriod(invalidEmail)).isEmpty();
  }

  static Stream<Arguments> invalidOptionSelections() {
    return Stream.of(
        Arguments.of("unknown", List.of("ws-ai", "ws-does-not-exist")),
        Arguments.of("inactive", List.of("ws-legacy")),
        Arguments.of("listed twice", List.of("ev-dinner", "meal-lunch", "ev-dinner")));
  }

  @ParameterizedTest(name = "AC-001-05 option {0}")
  @MethodSource("invalidOptionSelections")
  void AC_001_05_unknownInactiveOrDuplicateOptionIsRejected(String kind, List<String> optionIds)
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String email = (String) body.get("email");
    body.put("optionIds", optionIds);

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains("optionIds");
    assertNothingStoredOrSent(email);
  }

  static Stream<Arguments> consentsWithoutPrivacy() {
    return Stream.of(
        Arguments.of("missing", null),
        Arguments.of("empty", List.of()),
        Arguments.of("only optional", List.of("photos")));
  }

  @ParameterizedTest(name = "AC-001-06 consents {0}")
  @MethodSource("consentsWithoutPrivacy")
  void AC_001_06_missingMandatoryConsentIsRejected(String kind, List<String> consentIds)
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String email = (String) body.get("email");
    if (consentIds == null) {
      body.remove("consentIds");
    } else {
      body.put("consentIds", consentIds);
    }

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains("consents.privacy");
    assertNothingStoredOrSent(email);
  }

  @ParameterizedTest(name = "AC-001-07 token = [{0}]")
  @ValueSource(strings = {"missing", "", "test-fail", "forged-token"})
  void AC_001_07_missingOrRejectedRecaptchaTokenIsRejected(String token) throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String email = (String) body.get("email");
    if ("missing".equals(token)) {
      body.remove("recaptchaToken");
    } else {
      body.put("recaptchaToken", token);
    }

    MvcResult result = submit(body);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(errorFields(result)).contains("recaptchaToken");
    assertNothingStoredOrSent(email);
  }
}
