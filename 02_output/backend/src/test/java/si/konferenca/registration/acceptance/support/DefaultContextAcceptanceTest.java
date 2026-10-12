package si.konferenca.registration.acceptance.support;

import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Parent of the test classes that run against the default configuration (one shared context). */
public abstract class DefaultContextAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void defaultProperties(DynamicPropertyRegistry registry) {
    TestInfrastructure.register(registry, Map.of());
  }
}
