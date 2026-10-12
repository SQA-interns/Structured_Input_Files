package si.konferenca.registration.infrastructure.jsoncopy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.application.RegistrationCopyWriter;
import si.konferenca.registration.domain.Registration;
import tools.jackson.databind.json.JsonMapper;

/** Writes {@code <id>.json} atomically into the JSON copy directory (S6). */
public class FileRegistrationCopyWriter implements RegistrationCopyWriter {

  private static final Logger LOG = LoggerFactory.getLogger(FileRegistrationCopyWriter.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final Path directory;

  public FileRegistrationCopyWriter(Path directory) {
    this.directory = directory;
    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      // Not fatal here: readiness reports the directory down and every write fails (S6).
      LOG.warn("JSON copy directory cannot be created: {}", e.getClass().getSimpleName());
    }
  }

  @Override
  public byte[] write(Registration registration) {
    byte[] bytes = JSON.writeValueAsBytes(RegistrationCopy.of(registration));
    try {
      Path temp = Files.createTempFile(directory, ".registration-", ".tmp", ownerOnly());
      try {
        Files.write(temp, bytes);
        Files.move(temp, target(registration.id()), StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temp);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("JSON copy could not be written", e);
    }
    return bytes;
  }

  @Override
  public void delete(UUID registrationId) {
    try {
      Files.deleteIfExists(target(registrationId));
    } catch (IOException e) {
      throw new UncheckedIOException("JSON copy could not be removed", e);
    }
  }

  private Path target(UUID id) {
    return directory.resolve(id + ".json");
  }

  private static FileAttribute<?>[] ownerOnly() {
    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      return new FileAttribute<?>[] {
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
      };
    }
    return new FileAttribute<?>[0];
  }
}
