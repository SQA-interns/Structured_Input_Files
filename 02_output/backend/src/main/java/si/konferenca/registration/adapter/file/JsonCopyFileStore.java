package si.konferenca.registration.adapter.file;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.Ports.JsonCopyStore;
import si.konferenca.registration.application.StorageFailedException;
import si.konferenca.registration.domain.Registration;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Raw JSON copies as `<id>.json` in JSON_COPY_DIR (`registration-copy.schema.json`, BR-07). A copy
 * is written to a temporary file and moved into place, so no partial copy is ever visible.
 */
@Component
public class JsonCopyFileStore implements JsonCopyStore {

  private static final Logger LOG = LoggerFactory.getLogger(JsonCopyFileStore.class);
  private static final JsonMapper JSON =
      JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private final Path directory;

  public JsonCopyFileStore(@Value("${app.storage.json-copy-dir}") Path directory) {
    this.directory = directory;
  }

  @Override
  public byte[] write(Registration registration) {
    byte[] content = JSON.writeValueAsBytes(document(registration));
    Path target = directory.resolve(registration.id() + ".json");
    Path temporary = null;
    try {
      Files.createDirectories(directory);
      temporary = Files.createTempFile(directory, registration.id().toString(), ".tmp");
      Files.write(temporary, content);
      move(temporary, target);
      return content;
    } catch (IOException e) {
      deleteQuietly(temporary);
      throw new StorageFailedException(
          "JSON copy of registration " + registration.id() + " could not be written", e);
    }
  }

  @Override
  public void delete(UUID registrationId) {
    deleteQuietly(directory.resolve(registrationId + ".json"));
  }

  private static void move(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void deleteQuietly(Path file) {
    if (file == null) {
      return;
    }
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      LOG.warn("Could not delete {} ({})", file.getFileName(), e.getClass().getSimpleName());
    }
  }

  /** The copy in the field order of the schema; fields of the other type are absent. */
  static Map<String, Object> document(Registration r) {
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put("id", r.id().toString());
    doc.put("type", r.type().name());
    doc.put("registeredAt", Registration.format(r.registeredAt()));
    doc.put("firstName", r.firstName());
    doc.put("lastName", r.lastName());
    doc.put("email", r.email());
    putIfPresent(doc, "organization", r.organization());
    putIfPresent(doc, "studyInstitution", r.studyInstitution());
    putIfPresent(doc, "studyProgramme", r.studyProgramme());
    putIfPresent(doc, "studentId", r.studentId());
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
    doc.put("options", options);
    List<Map<String, Object>> consents =
        r.consents().stream()
            .map(
                c -> {
                  Map<String, Object> consent = new LinkedHashMap<>();
                  consent.put("id", c.id());
                  consent.put("text", c.text());
                  consent.put("givenAt", Registration.format(c.givenAt()));
                  return consent;
                })
            .toList();
    doc.put("consents", consents);
    return doc;
  }

  private static void putIfPresent(Map<String, Object> doc, String key, String value) {
    if (value != null) {
      doc.put(key, value);
    }
  }
}
