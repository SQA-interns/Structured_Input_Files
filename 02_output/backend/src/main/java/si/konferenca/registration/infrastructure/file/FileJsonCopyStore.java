package si.konferenca.registration.infrastructure.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.Timestamps;
import tools.jackson.databind.json.JsonMapper;

/** Writes {@code <dir>/<id>.json} (registration-copy.schema.json). */
public class FileJsonCopyStore implements JsonCopyStore {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final Path directory;

  public FileJsonCopyStore(Path directory) {
    this.directory = directory;
  }

  /** The exact bytes of the copy of a registration (UTF-8 JSON). */
  public static byte[] content(Registration r) {
    Map<String, Object> copy = new LinkedHashMap<>();
    copy.put("id", r.id().toString());
    copy.put("type", r.type().name());
    copy.put("registeredAt", Timestamps.format(r.registeredAt()));
    copy.put("firstName", r.firstName());
    copy.put("lastName", r.lastName());
    copy.put("email", r.email());
    putIfPresent(copy, "organization", r.organization());
    putIfPresent(copy, "studyInstitution", r.studyInstitution());
    putIfPresent(copy, "studyProgramme", r.studyProgramme());
    putIfPresent(copy, "studentId", r.studentId());
    List<Map<String, String>> options =
        r.options().stream()
            .map(o -> Map.of("id", o.id(), "name", o.name(), "category", o.category().name()))
            .map(LinkedHashMap::new)
            .map(m -> orderedOption(m))
            .toList();
    copy.put("options", options);
    copy.put(
        "consents",
        r.consents().stream()
            .map(c -> orderedConsent(c.id(), Timestamps.format(c.givenAt())))
            .toList());
    return JSON.writeValueAsBytes(copy);
  }

  private static Map<String, String> orderedOption(Map<String, String> option) {
    Map<String, String> ordered = new LinkedHashMap<>();
    ordered.put("id", option.get("id"));
    ordered.put("name", option.get("name"));
    ordered.put("category", option.get("category"));
    return ordered;
  }

  private static Map<String, String> orderedConsent(String id, String givenAt) {
    Map<String, String> ordered = new LinkedHashMap<>();
    ordered.put("id", id);
    ordered.put("givenAt", givenAt);
    return ordered;
  }

  private static void putIfPresent(Map<String, Object> copy, String key, String value) {
    if (value != null) {
      copy.put(key, value);
    }
  }

  @Override
  public void write(Registration registration) {
    Path target = directory.resolve(registration.id() + ".json");
    Path temporary = directory.resolve(registration.id() + ".json.tmp");
    try {
      Files.createDirectories(directory);
      Files.write(temporary, content(registration));
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException ignored) {
        // the temporary file is not a copy; nothing else to clean up
      }
      throw new JsonCopyWriteException(
          "JSON copy of registration " + registration.id() + " could not be written", e);
    }
  }
}
