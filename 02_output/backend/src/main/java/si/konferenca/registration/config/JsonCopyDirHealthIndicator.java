package si.konferenca.registration.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Readiness: the JSON copy directory exists and is writable (BR-07, S6, S12). */
@Component("jsonCopyDir")
class JsonCopyDirHealthIndicator implements HealthIndicator {

  private final Path directory;

  JsonCopyDirHealthIndicator(AppProperties properties) {
    this.directory = Path.of(properties.jsonCopyDir());
  }

  @Override
  public Health health() {
    boolean ok = Files.isDirectory(directory) && Files.isWritable(directory);
    return ok ? Health.up().build() : Health.down().build();
  }
}
