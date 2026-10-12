package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainModelTest {

  private static ConferenceOption option(String id, boolean active, RegistrationType... types) {
    return new ConferenceOption(id, id, Category.MEAL, active, EnumSet.copyOf(List.of(types)));
  }

  @Test
  void optionIsSelectableOnlyWhenActiveAndOpenToTheType() {
    ConferenceOption students = option("s", true, RegistrationType.STUDENT);

    assertThat(students.selectableBy(RegistrationType.STUDENT)).isTrue();
    assertThat(students.selectableBy(RegistrationType.EXTERNAL)).isFalse();
    assertThat(option("i", false, RegistrationType.STUDENT).selectableBy(RegistrationType.STUDENT))
        .isFalse();
  }

  @Test
  void catalogKeepsOrderListsActiveOptionsAndRejectsDuplicates() {
    OptionCatalog catalog =
        new OptionCatalog(
            new Consent("c", "t"),
            List.of(
                option("b", true, RegistrationType.EXTERNAL),
                option("a", false, RegistrationType.EXTERNAL),
                option("c", true, RegistrationType.EXTERNAL)));

    assertThat(catalog.activeOptions()).extracting(ConferenceOption::id).containsExactly("b", "c");
    assertThat(catalog.find("a")).isPresent();
    assertThat(catalog.find("z")).isEmpty();
    assertThat(catalog.consent()).isEqualTo(new Consent("c", "t"));
    assertThatThrownBy(
            () ->
                new OptionCatalog(
                    new Consent("c", "t"),
                    List.of(
                        option("d", true, RegistrationType.EXTERNAL),
                        option("d", true, RegistrationType.STUDENT))))
        .hasMessageContaining("'d'");
  }

  @Test
  void registrationNormalizesTheEmailAndCopiesItsOptions() {
    List<SelectedOption> options =
        new ArrayList<>(List.of(new SelectedOption("a", "A", Category.MEAL)));
    Registration r =
        new Registration(
            UUID.randomUUID(),
            RegistrationType.EXTERNAL,
            Instant.EPOCH,
            "A",
            "B",
            " Ana@Example.SI ",
            "X",
            null,
            null,
            null,
            options,
            new ConsentRecord("c", "t", Instant.EPOCH));
    options.clear();

    assertThat(r.normalizedEmail()).isEqualTo("ana@example.si");
    assertThat(r.selectedOptions()).hasSize(1);
  }

  @Test
  void labelsAreTheUserFacingNames() {
    assertThat(RegistrationType.EXTERNAL.label()).isEqualTo("External participant");
    assertThat(RegistrationType.STUDENT.label()).isEqualTo("Student");
    assertThat(Category.OTHER.label()).isEqualTo("Other activities");
  }

  @Test
  void validationExceptionKeepsAnUnmodifiableCopyOfTheErrors() {
    List<FieldError> errors = new ArrayList<>(List.of(new FieldError("a", "b")));
    ValidationException e = new ValidationException(errors);
    errors.clear();

    assertThat(e.errors()).containsExactly(new FieldError("a", "b"));
    assertThatThrownBy(() -> e.errors().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void formCopiesItsOptionList() {
    List<String> ids = new ArrayList<>(List.of("a"));
    RegistrationForm form = new RegistrationForm("EXTERNAL", "", "", "", "", "", "", "", ids, true);
    ids.add("b");

    assertThat(form.optionIds()).containsExactly("a");
    assertThat(
            new RegistrationForm(null, null, null, null, null, null, null, null, null, null)
                .optionIds())
        .isNull();
  }
}
