package si.konferenca.registration.acceptance;

import tools.jackson.databind.node.ObjectNode;

/**
 * Valid submissions as defined in `acceptance-criteria.md` ("valid external submission", "valid
 * student submission"), against the test configuration `acceptance/conference.json`.
 */
public final class Submissions {

  private Submissions() {}

  /** Valid external submission with one option per category available to externals. */
  public static ObjectNode validExternal() {
    ObjectNode n = AcceptanceEnvironment.JSON.createObjectNode();
    n.put("type", "EXTERNAL");
    n.put("firstName", "Ana");
    n.put("lastName", "Novak");
    n.put("email", "ana.novak@example.si");
    n.put("organization", "Institut Primer");
    n.putArray("optionIds").add("ws-a").add("ws-ext").add("ev-a").add("meal-a").add("ot-a");
    n.putObject("consents").put("privacy", true).put("photos", false);
    n.put("recaptchaToken", AcceptanceEnvironment.PASS_TOKEN);
    return n;
  }

  /** Valid student submission with options available to students. */
  public static ObjectNode validStudent() {
    ObjectNode n = AcceptanceEnvironment.JSON.createObjectNode();
    n.put("type", "STUDENT");
    n.put("firstName", "Luka");
    n.put("lastName", "Kranjc");
    n.put("email", "luka.kranjc@student.example.si");
    n.put("studyInstitution", "Univerza v Ljubljani");
    n.put("studyProgramme", "Računalništvo in informatika");
    n.put("studentId", "63210001");
    n.putArray("optionIds").add("ws-a").add("meal-a").add("ot-stu");
    n.putObject("consents").put("privacy", true);
    n.put("recaptchaToken", AcceptanceEnvironment.PASS_TOKEN);
    return n;
  }
}
