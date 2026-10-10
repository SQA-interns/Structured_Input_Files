package si.konferenca.registration.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import si.konferenca.registration.acceptance.support.TestEnvironment;
import si.konferenca.registration.application.DuplicateEmailException;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.GivenConsent;
import si.konferenca.registration.domain.AcceptedRegistration.Participant;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** The JPA adapter against PostgreSQL with the Flyway schema (integration level). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaRegistrationStoreIntegrationTest {

  @Autowired private RegistrationStore store;
  @Autowired private TransactionTemplate transactions;

  @DynamicPropertySource
  static void environment(DynamicPropertyRegistry registry) {
    TestEnvironment.register(registry, Map.of());
  }

  private static AcceptedRegistration registration(String email, Instant at) {
    return new AcceptedRegistration(
        UUID.randomUUID(),
        at,
        RegistrationType.STUDENT,
        new Participant("Ž", "Č", email, null, "UM", "RIT", "E1"),
        List.of(
            new SelectedOption("b", "Second", OptionCategory.MEAL),
            new SelectedOption("a", "First", OptionCategory.WORKSHOP)),
        List.of(new GivenConsent("data", "Data", at)));
  }

  @Test
  void insertsAndReadsBackInOrderWithSnapshots() {
    String email = "IT-" + UUID.randomUUID() + "@Example.si";
    Instant at = Instant.parse("2001-01-01T00:00:00.123Z");
    AcceptedRegistration r = registration(email, at);

    transactions.executeWithoutResult(s -> store.insert(r));

    assertThat(store.existsByNormalizedEmail(email.toLowerCase())).isTrue();
    assertThat(store.existsByNormalizedEmail(email)).isFalse();
    AcceptedRegistration first = store.findAllOrderedBySubmission().get(0);
    assertThat(first).isEqualTo(r);
    assertThat(first.options()).extracting(SelectedOption::id).containsExactly("b", "a");
  }

  @Test
  void secondInsertWithTheSameNormalizedEmailIsADuplicate() {
    String email = "dup-" + UUID.randomUUID() + "@example.si";
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    transactions.executeWithoutResult(s -> store.insert(registration(email, now)));

    assertThatThrownBy(
            () ->
                transactions.executeWithoutResult(
                    s -> store.insert(registration(email.toUpperCase(), now))))
        .isInstanceOf(DuplicateEmailException.class);
  }

  @Test
  void otherConstraintViolationsAreNotReportedAsDuplicates() {
    AcceptedRegistration invalid =
        new AcceptedRegistration(
            UUID.randomUUID(),
            Instant.now(),
            RegistrationType.STUDENT,
            new Participant(
                "A", "B", "x-" + UUID.randomUUID() + "@example.si", "IJS", null, null, null),
            List.of(),
            List.of());

    assertThatThrownBy(() -> transactions.executeWithoutResult(s -> store.insert(invalid)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
