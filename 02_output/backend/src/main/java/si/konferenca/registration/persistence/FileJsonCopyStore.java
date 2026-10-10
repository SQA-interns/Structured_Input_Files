package si.konferenca.registration.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.domain.AcceptedRegistration;

/**
 * Writes {@code registration-<reference>.json} into the configured directory: temp file, fsync,
 * atomic rename; an existing copy is never overwritten (BR-07).
 */
public class FileJsonCopyStore implements JsonCopyStore {

  private final Path directory;

  public FileJsonCopyStore(Path directory) {
    this.directory = directory;
  }

  @Override
  public byte[] write(AcceptedRegistration registration) {
    byte[] content = RegistrationJson.toBytes(registration);
    Path target = file(registration.reference());
    Path temp = null;
    try {
      Files.createDirectories(directory);
      if (Files.exists(target)) {
        throw new FileAlreadyExistsException(target.getFileName().toString());
      }
      temp = Files.createTempFile(directory, ".registration-", ".tmp");
      try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        while (buffer.hasRemaining()) {
          channel.write(buffer);
        }
        channel.force(true);
      }
      Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
      return content;
    } catch (IOException e) {
      deleteQuietly(temp);
      throw new UncheckedIOException("JSON copy could not be written", e);
    }
  }

  @Override
  public void delete(UUID reference) {
    deleteQuietly(file(reference));
  }

  private Path file(UUID reference) {
    return directory.resolve("registration-" + reference + ".json");
  }

  private static void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      // nothing more can be done; the copy is orphaned and harmless
    }
  }
}
