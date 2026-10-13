package si.konferenca.registration.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Component;
import si.konferenca.registration.RegistrationProperties;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.service.JsonCopyStore;

/** JSON copies as {@code <id>.json} in the configured directory, written atomically. */
@Component
public class FileJsonCopyStore implements JsonCopyStore {

  private final Path directory;

  public FileJsonCopyStore(RegistrationProperties properties) {
    this.directory = properties.jsonCopyDir();
  }

  @Override
  public byte[] write(Registration registration) {
    byte[] content = RegistrationJson.toBytes(registration);
    Path target = file(registration.id());
    Path temporary = null;
    try {
      Files.createDirectories(directory);
      temporary = Files.createTempFile(directory, registration.id().toString(), ".tmp");
      Files.write(temporary, content);
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
      return content;
    } catch (IOException e) {
      deleteQuietly(temporary);
      throw new UncheckedIOException("JSON copy could not be written", e);
    }
  }

  @Override
  public void delete(UUID id) {
    deleteQuietly(file(id));
  }

  private Path file(UUID id) {
    return directory.resolve(id + ".json");
  }

  private static void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      // Nothing more can be done; the copy directory is unusable.
    }
  }
}
