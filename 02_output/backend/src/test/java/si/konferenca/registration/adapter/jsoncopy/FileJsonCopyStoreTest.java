package si.konferenca.registration.adapter.jsoncopy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.adapter.Fixtures;
import si.konferenca.registration.domain.Registration;

class FileJsonCopyStoreTest {

  @TempDir Path dir;

  @Test
  void writesTheContractDocumentAndReturnsTheBytes() throws Exception {
    Registration r = Fixtures.external();
    FileJsonCopyStore store = new FileJsonCopyStore(dir.resolve("copies"));

    byte[] bytes = store.write(r);

    Path file = dir.resolve("copies").resolve(r.id() + ".json");
    assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
    DocumentContext doc = JsonPath.parse(new String(bytes, StandardCharsets.UTF_8));
    assertThat(doc.<Integer>read("$.schemaVersion")).isEqualTo(1);
    assertThat(doc.<String>read("$.type")).isEqualTo("EXTERNAL");
    assertThat(doc.<String>read("$.registeredAt")).isEqualTo("2026-10-11T08:30:00Z");
    assertThat(doc.<String>read("$.participant.firstName")).isEqualTo("Črt");
    assertThat(doc.<Map<String, Object>>read("$.participant")).doesNotContainKey("studentId");
    assertThat(doc.<String>read("$.options[1].category")).isEqualTo("MEAL");
    assertThat(doc.<String>read("$.options[1].name")).isEqualTo("Kosilo");
    assertThat(doc.<String>read("$.consents[0].givenAt")).isEqualTo("2026-10-11T08:30:00Z");
    assertThat(doc.<String>read("$.consents[0].text")).isEqualTo("Privacy text");
    try (var files = Files.list(dir.resolve("copies"))) {
      assertThat(files).hasSize(1);
    }
  }

  @Test
  void studentCopyHasOnlyStudentFields() {
    Map<String, Object> p =
        JsonPath.parse(
                new String(
                    new FileJsonCopyStore(dir).write(Fixtures.student()), StandardCharsets.UTF_8))
            .read("$.participant");

    assertThat(p)
        .containsOnlyKeys(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId");
  }

  @Test
  void deleteRemovesTheCopy() {
    FileJsonCopyStore store = new FileJsonCopyStore(dir);
    Registration r = Fixtures.student();
    store.write(r);

    store.delete(r.id());
    store.delete(r.id());

    assertThat(dir.resolve(r.id() + ".json")).doesNotExist();
  }

  @Test
  void failsWhenTheDirectoryIsAFile() throws Exception {
    Path notADir = dir.resolve("file");
    FileJsonCopyStore store = new FileJsonCopyStore(notADir);
    Files.delete(notADir);
    Files.writeString(notADir, "x");

    assertThatThrownBy(() -> store.write(Fixtures.student()))
        .isInstanceOf(UncheckedIOException.class);
    assertThatThrownBy(() -> new FileJsonCopyStore(notADir))
        .isInstanceOf(UncheckedIOException.class);
  }
}
