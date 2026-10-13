package si.konferenca.registration.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.RegistrationProperties;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class FileJsonCopyStoreTest {

  static final UUID ID = UUID.fromString("6f1c1b7e-2f7a-4c55-9d0e-3c1f7c2b9a10");
  static final Instant AT = Instant.parse("2026-10-13T08:00:00.120Z");

  static Registration student() {
    return new Registration(
        ID,
        AT,
        RegistrationType.STUDENT,
        "Špela",
        "Žnidaršič",
        "s@example.com",
        null,
        "Univerza",
        "Računalništvo",
        "6321",
        List.of(new Registration.SelectedOption("ws", "Workshop", OptionCategory.WORKSHOP)),
        List.of(new Registration.GivenConsent("privacy", "I agree", AT)));
  }

  static FileJsonCopyStore store(Path dir) {
    return new FileJsonCopyStore(
        new RegistrationProperties(
            "C",
            "c",
            dir,
            new RegistrationProperties.Mail("f@example.com", List.of()),
            new RegistrationProperties.Recaptcha(true, "", "", URI.create("http://x")),
            new RegistrationProperties.Organizer("u", "p", true),
            new RegistrationProperties.Cors(List.of()),
            new RegistrationProperties.RateLimit(1, 1, 1),
            100));
  }

  @Test
  void jsonCopyHasTheContractFieldsInOrderWithLfAndUnescapedText() {
    String json = new String(RegistrationJson.toBytes(student()), StandardCharsets.UTF_8);

    assertThat(json)
        .isEqualTo(
            """
            {
              "id" : "6f1c1b7e-2f7a-4c55-9d0e-3c1f7c2b9a10",
              "type" : "STUDENT",
              "registeredAt" : "2026-10-13T08:00:00.120Z",
              "firstName" : "Špela",
              "lastName" : "Žnidaršič",
              "email" : "s@example.com",
              "studyInstitution" : "Univerza",
              "studyProgramme" : "Računalništvo",
              "studentId" : "6321",
              "options" : [ {
                "id" : "ws",
                "name" : "Workshop",
                "category" : "WORKSHOP"
              } ],
              "consents" : [ {
                "id" : "privacy",
                "text" : "I agree",
                "givenAt" : "2026-10-13T08:00:00.120Z"
              } ]
            }""");
    assertThat(json).doesNotContain("\r", "organization");
  }

  @Test
  void externalCopyHasOrganizationOnly() {
    Registration external =
        new Registration(
            ID,
            AT,
            RegistrationType.EXTERNAL,
            "A",
            "B",
            "a@b.co",
            "Org",
            null,
            null,
            null,
            List.of(),
            List.of());

    String json = new String(RegistrationJson.toBytes(external), StandardCharsets.UTF_8);

    assertThat(json).contains("\"organization\" : \"Org\"", "\"options\" : [ ]");
    assertThat(json).doesNotContain("studyInstitution", "studyProgramme", "studentId");
  }

  @Test
  void writeCreatesTheDirectoryAndTheNamedFileAndReturnsItsBytes(@TempDir Path tmp)
      throws Exception {
    Path dir = tmp.resolve("nested/copies");

    byte[] written = store(dir).write(student());

    Path file = dir.resolve(ID + ".json");
    assertThat(Files.readAllBytes(file))
        .isEqualTo(written)
        .isEqualTo(RegistrationJson.toBytes(student()));
    try (Stream<Path> files = Files.list(dir)) {
      assertThat(files).containsExactly(file);
    }
  }

  @Test
  void deleteRemovesTheCopyAndIgnoresAMissingOne(@TempDir Path tmp) {
    FileJsonCopyStore store = store(tmp);
    store.write(student());

    store.delete(ID);
    store.delete(ID);

    assertThat(tmp.resolve(ID + ".json")).doesNotExist();
  }

  @Test
  void deleteOfANonEmptyDirectoryWithTheSameNameIsOnlyLogged(@TempDir Path tmp) throws Exception {
    Files.createDirectories(tmp.resolve(ID + ".json").resolve("inside"));

    store(tmp).delete(ID);

    assertThat(tmp.resolve(ID + ".json")).isDirectory();
  }

  @Test
  void writeFailsWhenTheDirectoryIsAFileAndLeavesNoTemporaryFile(@TempDir Path tmp)
      throws Exception {
    Path notADirectory = Files.createFile(tmp.resolve("file"));

    assertThatThrownBy(() -> store(notADirectory).write(student()))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessage("JSON copy could not be written");
  }

  @Test
  void failedMoveRemovesTheTemporaryFile(@TempDir Path tmp) throws Exception {
    Files.createDirectories(tmp.resolve(ID + ".json").resolve("blocks-the-move"));

    assertThatThrownBy(() -> store(tmp).write(student())).isInstanceOf(UncheckedIOException.class);

    try (Stream<Path> files = Files.list(tmp)) {
      assertThat(files).containsExactly(tmp.resolve(ID + ".json"));
    }
  }
}
