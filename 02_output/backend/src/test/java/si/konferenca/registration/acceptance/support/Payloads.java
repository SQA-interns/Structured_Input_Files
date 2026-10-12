package si.konferenca.registration.acceptance.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Valid registration requests (OpenAPI RegistrationRequest) that tests then vary. */
public final class Payloads {

  private Payloads() {}

  /** An address no other test uses. */
  public static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";
  }

  /** A valid external submission with one workshop and one event. */
  public static Map<String, Object> external(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", email);
    body.put("organization", "Acme Testing Ltd");
    body.put("optionIds", new ArrayList<>(List.of("ws-security", "ev-opening")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("recaptchaToken", TestInfrastructure.TEST_MODE_TOKEN);
    return body;
  }

  /** A valid student submission with one workshop and one meal. */
  public static Map<String, Object> student(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Bor");
    body.put("lastName", "Kranjc");
    body.put("email", email);
    body.put("studyInstitution", "University of Maribor");
    body.put("studyProgramme", "Computer Science");
    body.put("studentId", "63210001");
    body.put("optionIds", new ArrayList<>(List.of("ws-testing", "meal-lunch")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("recaptchaToken", TestInfrastructure.TEST_MODE_TOKEN);
    return body;
  }
}
