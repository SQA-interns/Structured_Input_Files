package si.konferenca.registration.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.GivenConsent;
import si.konferenca.registration.domain.AcceptedRegistration.Participant;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class FileJsonCopyStoreTest {

  private static final Instant AT = Instant.parse("2026-10-11T08:00:00.123Z");

  @TempDir Path dir;

  private static AcceptedRegistration student() {
    return new AcceptedRegistration(
        UUID.randomUUID(),
        AT,
        RegistrationType.STUDENT,
        new Participant("Luka", "Kranjc", "l@example.si", null, "UM", "Računalništvo", "E1"),
        List.of(new SelectedOption("ev", "Večer", OptionCategory.EVENT)),
        List.of(new GivenConsent("data", "Soglasje", AT)));
  }

  @Test
  void writesTheContractDocumentAndReturnsItsBytes() throws Exception {
    AcceptedRegistration r = student();
    Path nested = dir.resolve("a/b");

    byte[] written = new FileJsonCopyStore(nested).write(r);

    Path file = nested.resolve("registration-" + r.reference() + ".json");
    assertThat(Files.readAllBytes(file)).isEqualTo(written);
    JsonNode json =
        JsonMapper.builder().build().readTree(new String(written, StandardCharsets.UTF_8));
    assertThat(json.propertyNames())
        .containsExactly(
            "schemaVersion",
            "reference",
            "submittedAt",
            "type",
            "participant",
            "options",
            "consents");
    assertThat(json.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(json.get("submittedAt").asString()).isEqualTo("2026-10-11T08:00:00.123Z");
    assertThat(json.get("participant").propertyNames())
        .containsExactly(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId");
    assertThat(json.get("participant").get("studyProgramme").asString()).isEqualTo("Računalništvo");
    assertThat(json.get("options").get(0).propertyNames())
        .containsExactly("id", "name", "category");
    assertThat(json.get("options").get(0).get("category").asString()).isEqualTo("EVENT");
    assertThat(json.get("consents").get(0).get("givenAt").asString())
        .isEqualTo("2026-10-11T08:00:00.123Z");
    try (Stream<Path> files = Files.list(nested)) {
      assertThat(files).hasSize(1);
    }
  }

  @Test
  void neverOverwritesAnExistingCopy() throws Exception {
    AcceptedRegistration r = student();
    FileJsonCopyStore store = new FileJsonCopyStore(dir);
    Path file = dir.resolve("registration-" + r.reference() + ".json");
    Files.writeString(file, "original");

    assertThatThrownBy(() -> store.write(r)).isInstanceOf(UncheckedIOException.class);
    assertThat(Files.readString(file)).isEqualTo("original");
    try (Stream<Path> files = Files.list(dir)) {
      assertThat(files).hasSize(1);
    }
  }

  @Test
  void failsWhenTheDirectoryIsAFile() throws Exception {
    Path notADirectory = Files.writeString(dir.resolve("file"), "x");

    assertThatThrownBy(() -> new FileJsonCopyStore(notADirectory).write(student()))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessage("JSON copy could not be written");
  }

  @Test
  void deleteRemovesTheCopyAndIgnoresMissingOnes() {
    AcceptedRegistration r = student();
    FileJsonCopyStore store = new FileJsonCopyStore(dir);
    store.write(r);

    store.delete(r.reference());
    store.delete(r.reference());

    assertThat(dir.resolve("registration-" + r.reference() + ".json")).doesNotExist();
  }

  @Test
  void externalCopyHasOrganizationOnly() {
    AcceptedRegistration r =
        new AcceptedRegistration(
            UUID.randomUUID(),
            AT,
            RegistrationType.EXTERNAL,
            new Participant("A", "B", "a@x.si", "IJS", null, null, null),
            List.of(),
            List.of());

    JsonNode json = JsonMapper.builder().build().readTree(RegistrationJson.toBytes(r));

    assertThat(json.get("participant").propertyNames())
        .containsExactly("firstName", "lastName", "email", "organization");
    assertThat(json.get("options").isEmpty()).isTrue();
  }
}
