package si.konferenca.registration.infra;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Raw JSON copies on persistent storage (BR-07, docs/02_contracts/registration-copy.schema.json):
 * one UTF-8 file per registration, named after its id, written atomically.
 */
@Component
public class JsonCopyStore {

  private static final Logger LOG = LoggerFactory.getLogger(JsonCopyStore.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** Two-space indentation and LF line ends on every operating system. */
  private static final DefaultPrettyPrinter PRETTY =
      new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));

  private final Path directory;

  public JsonCopyStore(AppProperties properties) {
    this.directory = properties.jsonCopy().dir();
  }

  /** Writes the copy and returns its bytes; throws when the file cannot be written. */
  public byte[] write(Registration registration) {
    byte[] content = JSON.writer().with(PRETTY).writeValueAsBytes(toMap(registration));
    Path target = file(registration.getId());
    Path temporary = directory.resolve(registration.getId() + ".json.tmp");
    try {
      Files.createDirectories(directory);
      Files.write(temporary, content);
      try {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
      return content;
    } catch (IOException e) {
      deleteQuietly(temporary);
      throw new UncheckedIOException("JSON copy could not be written", e);
    }
  }

  /** Removes the copy of a registration whose database transaction did not commit. */
  public void delete(UUID id) {
    deleteQuietly(file(id));
  }

  private Path file(UUID id) {
    return directory.resolve(id + ".json");
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      LOG.warn("Could not delete {}", path.getFileName());
    }
  }

  static Map<String, Object> toMap(Registration registration) {
    Map<String, Object> copy = new LinkedHashMap<>();
    copy.put("id", registration.getId().toString());
    copy.put("type", registration.getType().name());
    copy.put("firstName", registration.getFirstName());
    copy.put("lastName", registration.getLastName());
    copy.put("email", registration.getEmail());
    putIfPresent(copy, "organization", registration.getOrganization());
    putIfPresent(copy, "studyInstitution", registration.getStudyInstitution());
    putIfPresent(copy, "studyProgramme", registration.getStudyProgramme());
    putIfPresent(copy, "studentId", registration.getStudentId());
    List<Map<String, Object>> options = new ArrayList<>();
    for (SelectedOption option : registration.getOptions()) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("id", option.optionId());
      entry.put("name", option.optionName());
      entry.put("category", option.category());
      options.add(entry);
    }
    copy.put("options", options);
    List<Map<String, Object>> consents = new ArrayList<>();
    for (GivenConsent consent : registration.getConsents()) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("id", consent.consentId());
      entry.put("text", consent.consentText());
      entry.put("givenAt", consent.givenAt().toString());
      consents.add(entry);
    }
    copy.put("consents", consents);
    copy.put("registeredAt", registration.getRegisteredAt().toString());
    return copy;
  }

  private static void putIfPresent(Map<String, Object> map, String key, String value) {
    if (value != null) {
      map.put(key, value);
    }
  }
}
