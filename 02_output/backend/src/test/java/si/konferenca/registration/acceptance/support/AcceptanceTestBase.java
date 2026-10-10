package si.konferenca.registration.acceptance.support;

import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Acceptance tests that share one running application with the default test environment. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayNameGeneration(AcIdDisplayNames.class)
public abstract class AcceptanceTestBase extends AcceptanceSupport {

  protected static final Mailpit MAILPIT = TestEnvironment.mailpit();

  @LocalServerPort private int port;

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry registry) {
    TestEnvironment.register(registry, Map.of());
  }

  @Override
  protected int port() {
    return port;
  }
}
