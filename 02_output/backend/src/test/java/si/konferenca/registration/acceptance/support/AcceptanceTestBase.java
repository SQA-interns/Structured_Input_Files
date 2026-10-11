package si.konferenca.registration.acceptance.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Acceptance tests with the default test properties ({@link TestEnvironment#baseProperties}). */
public abstract class AcceptanceTestBase extends AcceptanceTestSupport {

  @DynamicPropertySource
  static void baseProperties(DynamicPropertyRegistry registry) {
    TestEnvironment.baseProperties(CONFERENCE_CONFIG, JSON_COPY_DIR)
        .forEach((key, value) -> registry.add(key, () -> value));
  }
}
