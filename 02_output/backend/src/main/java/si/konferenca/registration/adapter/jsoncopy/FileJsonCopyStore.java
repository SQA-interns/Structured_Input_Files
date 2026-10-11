package si.konferenca.registration.adapter.jsoncopy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.JsonCopyStore;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** One UTF-8 file per registration (contract registration-copy.schema.json). */
public final class FileJsonCopyStore implements JsonCopyStore {

  private static final JsonMapper JSON =
      JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private final Path dir;

  public FileJsonCopyStore(Path dir) {
    this.dir = dir;
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new UncheckedIOException("JSON copy directory cannot be created", e);
    }
  }

  @Override
  public byte[] write(Registration registration) {
    byte[] bytes = JSON.writeValueAsBytes(document(registration));
    try {
      Files.createDirectories(dir);
      Path target = dir.resolve(registration.id() + ".json");
      Path temp = Files.createTempFile(dir, registration.id() + "-", ".tmp");
      Files.write(temp, bytes);
      try {
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
      }
      return bytes;
    } catch (IOException e) {
      throw new UncheckedIOException("JSON copy cannot be written", e);
    }
  }

  @Override
  public void delete(UUID id) {
    try {
      Files.deleteIfExists(dir.resolve(id + ".json"));
    } catch (IOException e) {
      throw new UncheckedIOException("JSON copy cannot be deleted", e);
    }
  }

  static Map<String, Object> document(Registration r) {
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put("schemaVersion", 1);
    doc.put("id", r.id().toString());
    doc.put("type", r.type().name());
    doc.put("registeredAt", r.registeredAt().toString());
    doc.put("participant", participant(r.participant()));
    List<Map<String, Object>> options = new ArrayList<>();
    for (SelectedOption o : r.options()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", o.id());
      m.put("name", o.name());
      m.put("category", o.category().name());
      options.add(m);
    }
    doc.put("options", options);
    List<Map<String, Object>> consents = new ArrayList<>();
    for (GivenConsent c : r.consents()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", c.id());
      m.put("text", c.text());
      m.put("givenAt", c.givenAt().toString());
      consents.add(m);
    }
    doc.put("consents", consents);
    return doc;
  }

  private static Map<String, Object> participant(Participant p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("firstName", p.firstName());
    m.put("lastName", p.lastName());
    m.put("email", p.email());
    putIfPresent(m, "organization", p.organization());
    putIfPresent(m, "studyInstitution", p.studyInstitution());
    putIfPresent(m, "studyProgramme", p.studyProgramme());
    putIfPresent(m, "studentId", p.studentId());
    return m;
  }

  private static void putIfPresent(Map<String, Object> m, String key, String value) {
    if (value != null) {
      m.put(key, value);
    }
  }
}
