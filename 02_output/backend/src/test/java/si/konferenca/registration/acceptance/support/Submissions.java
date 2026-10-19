package si.konferenca.registration.acceptance.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds request bodies of openapi.json RegistrationRequest for the configuration
 * conference-test.json.
 */
public final class Submissions {

  private Submissions() {}

  /** A unique address per submission, so emails of one test never mix with another's. */
  public static String uniqueEmail(String name) {
    return name + "." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
  }

  /** Valid external submission with one option per category available to externals. */
  public static Map<String, Object> validExternal() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", uniqueEmail("ana.novak"));
    body.put("organization", "Institut Jozef Stefan");
    body.put("optionIds", new ArrayList<>(List.of("ws-ai", "ev-dinner", "meal-lunch", "oth-tour")));
    body.put("consentIds", new ArrayList<>(List.of("privacy")));
    body.put("recaptchaToken", AcceptanceEnvironment.VALID_CAPTCHA);
    return body;
  }

  /** Valid student submission with one option per category available to students. */
  public static Map<String, Object> validStudent() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Kranjc");
    body.put("email", uniqueEmail("luka.kranjc"));
    body.put("studyInstitution", "Univerza v Ljubljani");
    body.put("studyProgramme", "Računalništvo in informatika");
    body.put("studentId", "63210001");
    body.put(
        "optionIds", new ArrayList<>(List.of("ws-ai", "ev-dinner", "meal-lunch", "oth-mentoring")));
    body.put("consentIds", new ArrayList<>(List.of("privacy")));
    body.put("recaptchaToken", AcceptanceEnvironment.VALID_CAPTCHA);
    return body;
  }
}
