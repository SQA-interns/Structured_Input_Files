package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** US-001 external participant registration, through {@code POST /api/registrations}. */
class US001ExternalRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-001-03 a valid external registration is accepted and returns its id")
  void ac001_03_validExternalRegistrationIsAccepted() throws Exception {
    Map<String, Object> request = Registrations.validExternal();

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    String id = response.json("$.id");
    assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    assertThat((String) response.json("$.type")).isEqualTo("EXTERNAL");
    assertThat((List<String>) response.json("$.selectedOptions[*].id"))
        .containsExactly("ws-a", "meal-lunch");
    assertThat(db.registration(id)).isNotNull();
  }

  @ParameterizedTest(name = "AC-001-04 {0} empty or blank is rejected")
  @ValueSource(strings = {"firstName", "lastName", "email", "organization"})
  void ac001_04_requiredFieldEmptyOrBlankIsRejected(String field) throws Exception {
    for (Object value : new Object[] {"", "   ", null}) {
      Map<String, Object> request = Registrations.validExternal();
      request.put(field, value);

      Response response = api.postRegistration(request);

      assertThat(response.status()).as("%s=%s: %s", field, value, response).isEqualTo(400);
      assertThat(response.errorFields()).contains(field);
    }
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-001-05 leading and trailing whitespace is not stored")
  void ac001_05_surroundingWhitespaceIsTrimmed() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    String email = (String) request.get("email");
    request.put("firstName", "  Ana ");
    request.put("lastName", "\tNovak  ");
    request.put("email", "  " + email + " ");
    request.put("organization", " Acme d.o.o.   ");

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    Map<String, Object> row = db.registration(response.json("$.id"));
    assertThat(row).isNotNull();
    assertThat(row.get("first_name")).isEqualTo("Ana");
    assertThat(row.get("last_name")).isEqualTo("Novak");
    assertThat(row.get("email")).isEqualTo(email);
    assertThat(row.get("organization")).isEqualTo("Acme d.o.o.");
  }

  @ParameterizedTest(name = "AC-001-06 invalid email \"{0}\" is rejected")
  @ValueSource(strings = {"plainaddress", "ana@", "@example.si", "ana@example", "ana novak@x.si"})
  void ac001_06_invalidEmailIsRejected(String email) throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("email", email);

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("email");
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-001-07 Slovenian characters are accepted and stored unchanged")
  void ac001_07_slovenianCharactersAreStoredUnchanged() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("firstName", "Žiga");
    request.put("lastName", "Čebašek");
    request.put("organization", "Šolski center Škofja Loka, ČŠŽ čšž");

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    Map<String, Object> row = db.registration(response.json("$.id"));
    assertThat(row).isNotNull();
    assertThat(row.get("first_name")).isEqualTo("Žiga");
    assertThat(row.get("last_name")).isEqualTo("Čebašek");
    assertThat(row.get("organization")).isEqualTo("Šolski center Škofja Loka, ČŠŽ čšž");
  }

  @Test
  @DisplayName("AC-001-08 an unknown option id is rejected")
  void ac001_08_unknownOptionIsRejected() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("optionIds", List.of("ws-a", "does-not-exist"));

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("optionIds");
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-001-09 an inactive option is rejected")
  void ac001_09_inactiveOptionIsRejected() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("optionIds", List.of("ev-old"));

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("optionIds");
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-001-10 the same option selected twice is rejected")
  void ac001_10_duplicateOptionIsRejected() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("optionIds", List.of("ws-a", "ws-a"));

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(response.errorFields()).contains("optionIds");
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-001-11 several options in one category, or none, are accepted")
  void ac001_11_severalOptionsInOneCategoryOrNoneAreAccepted() throws Exception {
    Map<String, Object> several = Registrations.validExternal();
    several.put("optionIds", List.of("ws-a", "ws-b", "ws-ext"));
    Map<String, Object> none = Registrations.validExternal();
    none.put("optionIds", List.of());

    Response severalResponse = api.postRegistration(several);
    Response noneResponse = api.postRegistration(none);

    assertThat(severalResponse.status()).as(severalResponse.toString()).isEqualTo(201);
    assertThat(db.optionIds(severalResponse.json("$.id")))
        .containsExactly("ws-a", "ws-b", "ws-ext");
    assertThat(noneResponse.status()).as(noneResponse.toString()).isEqualTo(201);
    assertThat(db.optionIds(noneResponse.json("$.id"))).isEmpty();
  }

  @Test
  @DisplayName("AC-001-12 a registration without the mandatory consent is rejected")
  void ac001_12_missingConsentIsRejected() throws Exception {
    Map<String, Object> notGiven = Registrations.validExternal();
    notGiven.put("consentGiven", false);
    Map<String, Object> absent = Registrations.validExternal();
    absent.remove("consentGiven");

    Response notGivenResponse = api.postRegistration(notGiven);
    Response absentResponse = api.postRegistration(absent);

    assertThat(notGivenResponse.status()).as(notGivenResponse.toString()).isEqualTo(400);
    assertThat(notGivenResponse.errorFields()).contains("consentGiven");
    assertThat(absentResponse.status()).as(absentResponse.toString()).isEqualTo(400);
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-001-13 a missing or rejected anti-automation token is rejected (test mode)")
  void ac001_13_missingOrRejectedTokenIsRejected() throws Exception {
    Map<String, Object> missing = Registrations.validExternal();
    missing.remove("recaptchaToken");
    Map<String, Object> blank = Registrations.validExternal();
    blank.put("recaptchaToken", " ");
    Map<String, Object> wrong = Registrations.validExternal();
    wrong.put("recaptchaToken", "not-the-test-token");

    for (Map<String, Object> request : List.of(missing, blank, wrong)) {
      Response response = api.postRegistration(request);
      assertThat(response.status()).as(response.toString()).isEqualTo(400);
    }
    Response wrongResponse = api.postRegistration(wrong);
    assertThat(wrongResponse.errorFields()).contains("recaptchaToken");
    assertNothingStoredCopiedOrSent();
  }

  @Test
  @DisplayName("AC-001-14 a second registration with the same email is rejected")
  void ac001_14_duplicateEmailIsRejected() throws Exception {
    Map<String, Object> first = Registrations.validExternal();
    String email = (String) first.get("email");
    Response firstResponse = api.postRegistration(first);
    assertThat(firstResponse.status()).as(firstResponse.toString()).isEqualTo(201);
    mailbox.awaitMessages(3);
    mailbox.deleteAll();

    Map<String, Object> second = Registrations.validExternal();
    second.put("email", "  " + email.toUpperCase(java.util.Locale.ROOT) + " ");
    Response secondResponse = api.postRegistration(second);

    assertThat(secondResponse.status()).as(secondResponse.toString()).isEqualTo(409);
    assertThat(secondResponse.errorFields()).contains("email");
    assertThat(db.countRegistrations()).isEqualTo(1);
    assertThat(copies.files()).hasSize(1);
    assertThat(mailbox.messagesAfterSettling()).isEmpty();
  }
}
