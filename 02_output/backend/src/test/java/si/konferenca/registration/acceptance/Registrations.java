package si.konferenca.registration.acceptance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Request bodies for {@code POST /api/registrations} (schema {@code RegistrationRequest}). */
final class Registrations {

  static final String TEST_MODE_TOKEN = "test-mode-pass";
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  private Registrations() {}

  static String uniqueEmail() {
    return "participant" + SEQUENCE.incrementAndGet() + "@example.si";
  }

  /** A valid external registration selecting one active option open to external participants. */
  static Map<String, Object> validExternal() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", uniqueEmail());
    body.put("organization", "Acme d.o.o.");
    body.put("optionIds", new ArrayList<>(List.of("ws-a", "meal-lunch")));
    body.put("consentGiven", true);
    body.put("recaptchaToken", TEST_MODE_TOKEN);
    return body;
  }

  /** A valid student registration selecting options open to students. */
  static Map<String, Object> validStudent() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Kranjc");
    body.put("email", uniqueEmail());
    body.put("studyInstitution", "University of Ljubljana");
    body.put("studyProgramme", "Computer Science");
    body.put("studentId", "63210001");
    body.put("optionIds", new ArrayList<>(List.of("ws-b", "other-fair")));
    body.put("consentGiven", true);
    body.put("recaptchaToken", TEST_MODE_TOKEN);
    return body;
  }
}
