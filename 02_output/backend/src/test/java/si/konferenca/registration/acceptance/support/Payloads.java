package si.konferenca.registration.acceptance.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Registration request bodies (openapi.json RegistrationRequest) for the acceptance tests. */
public final class Payloads {

  public static final String EXTERNAL_FIELDS_ORGANIZATION = "organization";
  public static final List<String> EXTERNAL_REQUIRED =
      List.of("firstName", "lastName", "email", "organization");
  public static final List<String> STUDENT_REQUIRED =
      List.of("firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId");

  private Payloads() {}

  public static String unique() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  /** A valid external submission with a unique last name and email. */
  public static Map<String, Object> external() {
    String u = unique();
    Map<String, Object> p = new LinkedHashMap<>();
    p.put("type", "EXTERNAL");
    p.put("firstName", "Ana");
    p.put("lastName", "Novak-" + u);
    p.put("email", "ana." + u + "@example.test");
    p.put("organization", "Institut Jožef Stefan");
    p.put("optionIds", new ArrayList<>(List.of("ws-ai", "meal-lunch")));
    p.put("consentIds", new ArrayList<>(List.of("privacy")));
    p.put("captchaToken", TestEnvironment.TEST_CAPTCHA_TOKEN);
    return p;
  }

  /** A valid student submission with a unique last name and email. */
  public static Map<String, Object> student() {
    String u = unique();
    Map<String, Object> p = new LinkedHashMap<>();
    p.put("type", "STUDENT");
    p.put("firstName", "Luka");
    p.put("lastName", "Kranjc-" + u);
    p.put("email", "luka." + u + "@example.test");
    p.put("studyInstitution", "Univerza v Ljubljani");
    p.put("studyProgramme", "Računalništvo in informatika");
    p.put("studentId", "632" + u.substring(0, 5));
    p.put("optionIds", new ArrayList<>(List.of("ws-ai", "ev-dinner")));
    p.put("consentIds", new ArrayList<>(List.of("privacy")));
    p.put("captchaToken", TestEnvironment.TEST_CAPTCHA_TOKEN);
    return p;
  }

  public static Map<String, Object> with(Map<String, Object> base, String key, Object value) {
    Map<String, Object> copy = new LinkedHashMap<>(base);
    copy.put(key, value);
    return copy;
  }

  public static Map<String, Object> without(Map<String, Object> base, String key) {
    Map<String, Object> copy = new LinkedHashMap<>(base);
    copy.remove(key);
    return copy;
  }
}
