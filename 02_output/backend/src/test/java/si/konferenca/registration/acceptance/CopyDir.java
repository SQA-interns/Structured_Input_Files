package si.konferenca.registration.acceptance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Stream;

/** The JSON copy directory (contract {@code registration-copy.schema.json}). */
final class CopyDir {

  private final Path dir;

  CopyDir(Path dir) {
    this.dir = dir;
  }

  Path path() {
    return dir;
  }

  void reset() throws IOException {
    makeWritable();
    try (Stream<Path> files = Files.list(dir)) {
      for (Path f : files.toList()) {
        Files.deleteIfExists(f);
      }
    }
  }

  List<Path> files() throws IOException {
    try (Stream<Path> files = Files.list(dir)) {
      return files.toList();
    }
  }

  Path copyOf(String registrationId) {
    return dir.resolve(registrationId + ".json");
  }

  void makeReadOnly() throws IOException {
    Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("r-xr-xr-x"));
  }

  void makeWritable() throws IOException {
    Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"));
  }
}
