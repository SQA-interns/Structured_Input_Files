package si.konferenca.registration.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class RegistrationStoreTest {

  static final Instant AT = Instant.parse("2026-10-13T08:00:00.120Z");
  static final Registration STUDENT =
      new Registration(
          UUID.randomUUID(),
          AT,
          RegistrationType.STUDENT,
          "Bor",
          "Kranjc",
          "b@c.co",
          null,
          "Uni",
          "CS",
          "123",
          List.of(new Registration.SelectedOption("ws", "W", OptionCategory.WORKSHOP)),
          List.of(new Registration.GivenConsent("privacy", "P", AT)));

  private final RegistrationRepository repository = mock(RegistrationRepository.class);
  private final RegistrationStore store = new RegistrationStore(repository);

  @Test
  void insertMapsEveryFieldAndFlushes() {
    store.insert(STUDENT);

    ArgumentCaptor<RegistrationEntity> captor = ArgumentCaptor.forClass(RegistrationEntity.class);
    verify(repository).saveAndFlush(captor.capture());
    RegistrationEntity e = captor.getValue();
    assertThat(e.id()).isEqualTo(STUDENT.id());
    assertThat(e.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(e.firstName()).isEqualTo("Bor");
    assertThat(e.lastName()).isEqualTo("Kranjc");
    assertThat(e.email()).isEqualTo("b@c.co");
    assertThat(e.organization()).isNull();
    assertThat(e.studyInstitution()).isEqualTo("Uni");
    assertThat(e.studyProgramme()).isEqualTo("CS");
    assertThat(e.studentId()).isEqualTo("123");
    assertThat(e.registeredAt()).isEqualTo(AT);
    assertThat(e.options())
        .singleElement()
        .satisfies(
            o -> {
              assertThat(o.optionId()).isEqualTo("ws");
              assertThat(o.optionName()).isEqualTo("W");
              assertThat(o.optionCategory()).isEqualTo(OptionCategory.WORKSHOP);
            });
    assertThat(e.consents())
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.consentId()).isEqualTo("privacy");
              assertThat(c.consentText()).isEqualTo("P");
              assertThat(c.givenAt()).isEqualTo(AT);
            });
  }

  @Test
  void findAllMapsEntitiesBackInRepositoryOrder() {
    store.insert(STUDENT);
    ArgumentCaptor<RegistrationEntity> captor = ArgumentCaptor.forClass(RegistrationEntity.class);
    verify(repository).saveAndFlush(captor.capture());
    RegistrationEntity external =
        new RegistrationEntity(
            UUID.randomUUID(),
            RegistrationType.EXTERNAL,
            "Ana",
            "Novak",
            "a@b.co",
            "Org",
            null,
            null,
            null,
            AT.plusSeconds(1),
            List.of(),
            List.of());
    when(repository.findAllByOrderByRegisteredAtAsc())
        .thenReturn(List.of(captor.getValue(), external));

    List<Registration> all = store.findAllInOrder();

    assertThat(all.get(0)).isEqualTo(STUDENT);
    assertThat(all.get(1).organization()).isEqualTo("Org");
    assertThat(all.get(1).type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(all.get(1).options()).isEmpty();
  }

  @Test
  void jpaConstructorsExistForHibernate() {
    assertThat(new RegistrationEntity().options()).isEmpty();
    assertThat(new RegistrationEntity.OptionRow().optionId()).isNull();
    assertThat(new RegistrationEntity.ConsentRow().consentId()).isNull();
  }
}
