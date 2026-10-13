package si.konferenca.registration.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import si.konferenca.registration.domain.Registration;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** The JSON copy format (contract registration-copy.schema.json); UTF-8, nothing escaped. */
final class RegistrationJson {

  private static final JsonMapper JSON =
      JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private RegistrationJson() {}

  static byte[] toBytes(Registration r) {
    Map<String, Object> copy = new LinkedHashMap<>();
    copy.put("id", r.id().toString());
    copy.put("type", r.type().name());
    copy.put("registeredAt", r.registeredAt().toString());
    copy.put("firstName", r.firstName());
    copy.put("lastName", r.lastName());
    copy.put("email", r.email());
    putIfPresent(copy, "organization", r.organization());
    putIfPresent(copy, "studyInstitution", r.studyInstitution());
    putIfPresent(copy, "studyProgramme", r.studyProgramme());
    putIfPresent(copy, "studentId", r.studentId());
    List<Map<String, Object>> options =
        r.options().stream()
            .map(
                o -> {
                  Map<String, Object> option = new LinkedHashMap<>();
                  option.put("id", o.id());
                  option.put("name", o.name());
                  option.put("category", o.category().name());
                  return option;
                })
            .toList();
    copy.put("options", options);
    List<Map<String, Object>> consents =
        r.consents().stream()
            .map(
                c -> {
                  Map<String, Object> consent = new LinkedHashMap<>();
                  consent.put("id", c.id());
                  consent.put("text", c.text());
                  consent.put("givenAt", c.givenAt().toString());
                  return consent;
                })
            .toList();
    copy.put("consents", consents);
    return JSON.writeValueAsBytes(copy);
  }

  private static void putIfPresent(Map<String, Object> copy, String key, String value) {
    if (value != null) {
      copy.put(key, value);
    }
  }
}
