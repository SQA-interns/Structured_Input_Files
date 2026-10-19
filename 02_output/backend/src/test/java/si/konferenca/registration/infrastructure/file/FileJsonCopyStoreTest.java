package si.konferenca.registration.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Covers AC-005-02, AC-005-03, AC-005-04. */
class FileJsonCopyStoreTest {

  @TempDir Path dir;

  static final Instant TIME = Instant.parse("2026-10-19T12:00:00.120Z");

  static Registration external() {
    return new Registration(
        UUID.fromString("7d1c6a52-3f0e-4b8e-9d55-2a4f0c9b1e11"),
        RegistrationType.EXTERNAL,
        TIME,
        "Žiga",
        "Šuštaršič",
        "ziga@example.com",
        "Fakulteta ČŠŽ čšž",
        null,
        null,
        null,
        List.of(new ConferenceOption("ws-a", "Delavnica č", OptionCategory.WORKSHOP, true, null)),
        List.of(new GivenConsent("privacy", TIME)));
  }

  static Registration student() {
    return new Registration(
        UUID.randomUUID(),
        RegistrationType.STUDENT,
        Instant.parse("2026-10-19T12:00:00Z"),
        "Luka",
        "Kranjc",
        "luka@example.com",
        null,
        "Univerza",
        "Informatika",
        "6321",
        List.of(),
        List.of());
  }

  @Test
  void AC_005_02_copyIsNamedAfterTheIdAndHoldsTheRegistration() throws Exception {
    new FileJsonCopyStore(dir).write(external());

    Path file = dir.resolve("7d1c6a52-3f0e-4b8e-9d55-2a4f0c9b1e11.json");
    assertThat(file).isRegularFile();
    JsonNode json = JsonMapper.builder().build().readTree(Files.readString(file));
    assertThat(json.path("id").asString()).isEqualTo("7d1c6a52-3f0e-4b8e-9d55-2a4f0c9b1e11");
    assertThat(json.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(json.path("registeredAt").asString()).isEqualTo("2026-10-19T12:00:00.120Z");
    assertThat(json.path("email").asString()).isEqualTo("ziga@example.com");
    assertThat(json.path("organization").asString()).isEqualTo("Fakulteta ČŠŽ čšž");
    assertThat(json.has("studyInstitution")).isFalse();
    assertThat(json.has("studentId")).isFalse();
    assertThat(json.path("options").get(0).path("id").asString()).isEqualTo("ws-a");
    assertThat(json.path("options").get(0).path("name").asString()).isEqualTo("Delavnica č");
    assertThat(json.path("options").get(0).path("category").asString()).isEqualTo("WORKSHOP");
    assertThat(json.path("consents").get(0).path("id").asString()).isEqualTo("privacy");
    assertThat(json.path("consents").get(0).path("givenAt").asString())
        .isEqualTo("2026-10-19T12:00:00.120Z");
  }

  @Test
  void AC_005_02_studentCopyHasStudentFieldsAndMillisecondTimes() throws Exception {
    Registration student = student();
    new FileJsonCopyStore(dir).write(student);

    JsonNode json =
        JsonMapper.builder()
            .build()
            .readTree(Files.readString(dir.resolve(student.id() + ".json")));
    assertThat(json.path("registeredAt").asString()).isEqualTo("2026-10-19T12:00:00.000Z");
    assertThat(json.path("studyInstitution").asString()).isEqualTo("Univerza");
    assertThat(json.path("studyProgramme").asString()).isEqualTo("Informatika");
    assertThat(json.path("studentId").asString()).isEqualTo("6321");
    assertThat(json.has("organization")).isFalse();
    assertThat(json.path("options").isArray()).isTrue();
    assertThat(json.path("consents").isArray()).isTrue();
  }

  @Test
  void AC_005_04_copyIsUtf8AndEqualsTheContentBytes() throws Exception {
    new FileJsonCopyStore(dir).write(external());

    byte[] written = Files.readAllBytes(dir.resolve(external().id() + ".json"));
    assertThat(written).isEqualTo(FileJsonCopyStore.content(external()));
    assertThat(new String(written, StandardCharsets.UTF_8)).contains("Šuštaršič", "Žiga");
  }

  @Test
  void AC_005_02_missingDirectoryIsCreated() {
    Path nested = dir.resolve("a").resolve("b");

    new FileJsonCopyStore(nested).write(external());

    assertThat(nested.resolve(external().id() + ".json")).isRegularFile();
  }

  @Test
  void AC_005_03_unwritableDirectoryFailsTheWrite() throws Exception {
    Path notADirectory = dir.resolve("file");
    Files.writeString(notADirectory, "x");

    assertThatThrownBy(() -> new FileJsonCopyStore(notADirectory).write(external()))
        .isInstanceOf(JsonCopyWriteException.class)
        .hasMessageContaining(external().id().toString());
  }
}
