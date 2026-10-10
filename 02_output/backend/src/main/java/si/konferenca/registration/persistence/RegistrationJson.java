package si.konferenca.registration.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.Participant;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** The JSON copy document (contract registration-copy.schema.json, schemaVersion 1), UTF-8. */
final class RegistrationJson {

  private static final JsonMapper JSON =
      JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private RegistrationJson() {}

  static byte[] toBytes(AcceptedRegistration r) {
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("schemaVersion", 1);
    document.put("reference", r.reference().toString());
    document.put("submittedAt", r.submittedAt().toString());
    document.put("type", r.type().name());
    document.put("participant", participant(r.participant()));
    document.put(
        "options",
        r.options().stream()
            .map(
                o ->
                    ordered(
                        List.of("id", o.id(), "name", o.name(), "category", o.category().name())))
            .toList());
    document.put(
        "consents",
        r.consents().stream()
            .map(
                c ->
                    ordered(
                        List.of("id", c.id(), "text", c.text(), "givenAt", c.givenAt().toString())))
            .toList());
    return JSON.writeValueAsBytes(document);
  }

  private static Map<String, Object> participant(Participant p) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("firstName", p.firstName());
    map.put("lastName", p.lastName());
    map.put("email", p.email());
    putIfPresent(map, "organization", p.organization());
    putIfPresent(map, "studyInstitution", p.studyInstitution());
    putIfPresent(map, "studyProgramme", p.studyProgramme());
    putIfPresent(map, "studentId", p.studentId());
    return map;
  }

  private static void putIfPresent(Map<String, Object> map, String key, String value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private static Map<String, Object> ordered(List<String> keysAndValues) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.size(); i += 2) {
      map.put(keysAndValues.get(i), keysAndValues.get(i + 1));
    }
    return map;
  }
}
