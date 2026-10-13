package si.konferenca.registration.adapter.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.TestRegistrations;
import si.konferenca.registration.application.StorageFailedException;
import si.konferenca.registration.domain.Registration;

/** BR-07, AC-005-02, AC-005-03: raw JSON copies. */
class JsonCopyFileStoreTest {

  @TempDir Path dir;

  @Test
  void copyIsWrittenAsIdJsonAndTheBytesAreReturned() throws Exception {
    Registration r = TestRegistrations.external();
    Path copies = dir.resolve("nested/copies");

    byte[] bytes = new JsonCopyFileStore(copies).write(r);

    Path file = copies.resolve(r.id() + ".json");
    assertThat(file).exists();
    assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
    String json = new String(bytes, StandardCharsets.UTF_8);
    assertThat(json)
        .contains("\"firstName\" : \"Žiga\"")
        .contains("\"registeredAt\" : \"2026-10-13T20:15:03.120Z\"")
        .contains("\"organization\" : \"Šola <b>\"")
        .doesNotContain("studentId");
    assertThat(json.indexOf("\"id\"")).isLessThan(json.indexOf("\"type\""));
    assertThat(json.indexOf("\"email\"")).isLessThan(json.indexOf("\"organization\""));
    assertThat(json.indexOf("\"options\"")).isLessThan(json.indexOf("\"consents\""));
    try (var files = Files.list(copies)) {
      assertThat(files).containsExactly(file);
    }
  }

  @Test
  void studentCopyHasOnlyStudentFields() {
    String json =
        new String(
            new JsonCopyFileStore(dir).write(TestRegistrations.student()), StandardCharsets.UTF_8);
    assertThat(json)
        .contains("\"studyInstitution\" : \"Uni\"", "\"studyProgramme\" : \"CS\"")
        .contains("\"studentId\" : \"63\"", "\"options\" : [ ]", "\"consents\" : [ ]")
        .doesNotContain("organization");
  }

  @Test
  void documentHoldsOptionsAndConsentsWithTheirDetails() {
    var doc = JsonCopyFileStore.document(TestRegistrations.external());
    assertThat(doc.keySet())
        .containsExactly(
            "id",
            "type",
            "registeredAt",
            "firstName",
            "lastName",
            "email",
            "organization",
            "options",
            "consents");
    assertThat(doc.get("options").toString())
        .isEqualTo(
            "[{id=ws, name=Workshop, category=WORKSHOP}, {id=ev, name=Event, category=EVENT},"
                + " {id=ws2, name=Workshop 2, category=WORKSHOP}]");
    assertThat(doc.get("consents").toString())
        .isEqualTo("[{id=privacy, text=I agree, givenAt=2026-10-13T20:15:03.120Z}]");
  }

  @Test
  void unwritableDirectoryFailsAndLeavesNothingBehind() throws Exception {
    Path notADirectory = Files.createFile(dir.resolve("file"));
    JsonCopyFileStore store = new JsonCopyFileStore(notADirectory);

    assertThatThrownBy(() -> store.write(TestRegistrations.external()))
        .isInstanceOf(StorageFailedException.class)
        .hasMessageContaining("could not be written");
    try (var files = Files.list(dir)) {
      assertThat(files).containsExactly(notADirectory);
    }
  }

  @Test
  void failedMoveRemovesTheTemporaryFile() throws Exception {
    Registration r = TestRegistrations.external();
    Files.createDirectories(dir.resolve(r.id() + ".json").resolve("blocker"));

    assertThatThrownBy(() -> new JsonCopyFileStore(dir).write(r))
        .isInstanceOf(StorageFailedException.class);
    try (var files = Files.list(dir)) {
      assertThat(files).containsExactly(dir.resolve(r.id() + ".json"));
    }
  }

  @Test
  void deleteRemovesACopyAndIgnoresMissingOnes() throws Exception {
    Registration r = TestRegistrations.external();
    JsonCopyFileStore store = new JsonCopyFileStore(dir);
    store.write(r);

    store.delete(r.id());
    store.delete(UUID.randomUUID());

    assertThat(dir.resolve(r.id() + ".json")).doesNotExist();
  }

  @Test
  void deleteFailureIsOnlyLogged() throws Exception {
    Registration r = TestRegistrations.external();
    Path blocked = dir.resolve(r.id() + ".json");
    Files.createDirectories(blocked.resolve("child"));

    new JsonCopyFileStore(dir).delete(r.id());

    assertThat(blocked).exists();
  }
}
