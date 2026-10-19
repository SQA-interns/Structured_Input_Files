package si.konferenca.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/**
 * Covers AC-005-03: a failing insert fails inside {@code save}, before the JSON copy is written
 * (AR-05; phase 5 survivors).
 */
@SpringBootTest
class JpaRegistrationFlushIntegrationTest {

  @Autowired RegistrationStore store;
  @Autowired TransactionTemplate transactions;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    AcceptanceEnvironment.register(registry, Map.of());
  }

  @BeforeEach
  void clean() {
    AcceptanceTestBase.clearStorage(AcceptanceEnvironment.jdbc());
  }

  @Test
  void AC_005_03_aDuplicateIdFailsInSaveItself() {
    UUID id = UUID.randomUUID();
    Registration registration =
        new Registration(
            id,
            RegistrationType.EXTERNAL,
            Instant.parse("2026-10-19T12:00:00Z"),
            "Ana",
            "Novak",
            "ana@example.com",
            "IJS",
            null,
            null,
            null,
            List.of(),
            List.of());
    store.save(registration);

    Boolean failedInSave =
        transactions.execute(
            status -> {
              status.setRollbackOnly();
              try {
                store.save(registration);
                return false;
              } catch (RuntimeException e) {
                return true;
              }
            });

    assertThat(failedInSave).isTrue();
  }
}
