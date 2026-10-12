package si.konferenca.registration.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.config.AppProperties;

class JsonCopyStoreTest {

  @TempDir Path temp;

  private static JsonCopyStore store(Path dir) {
    return new JsonCopyStore(
        new AppProperties(
            "test",
            new AppProperties.Conference("classpath:conference.json", "Conf"),
            new AppProperties.JsonCopy(dir),
            new AppProperties.Recaptcha(true, "", "", "http://localhost"),
            new AppProperties.Organizer("o", "p", List.of("o@example.org"), true),
            new AppProperties.Mail("from@example.org"),
            new AppProperties.Limits(1, 1, 1, 100),
            new AppProperties.Cors("")));
  }

  @Test
  void writesTheExternalRegistrationAsJsonWithLineFeeds() throws IOException {
    Path dir = temp.resolve("nested/copies");

    byte[] written = store(dir).write(Registrations.external());

    Path file = dir.resolve(Registrations.EXTERNAL_ID + ".json");
    assertThat(Files.readAllBytes(file)).isEqualTo(written);
    String json = new String(written, StandardCharsets.UTF_8);
    assertThat(json).doesNotContain("\r").contains("\n  \"id\"");
    Map<String, Object> root = JsonPath.read(json, "$");
    assertThat(root.keySet())
        .containsExactly(
            "id",
            "type",
            "firstName",
            "lastName",
            "email",
            "organization",
            "options",
            "consents",
            "registeredAt");
    assertThat(root)
        .containsEntry("id", Registrations.EXTERNAL_ID.toString())
        .containsEntry("type", "EXTERNAL")
        .containsEntry("organization", "Institut")
        .containsEntry("registeredAt", "2026-10-12T10:15:30.123456Z");
    assertThat(JsonPath.<List<String>>read(json, "$.options[*].id"))
        .containsExactly("ws-a", "meal-b");
    assertThat(JsonPath.<List<String>>read(json, "$.options[*].name"))
        .containsExactly("Workshop A", "Lunch");
    assertThat(JsonPath.<List<String>>read(json, "$.options[*].category"))
        .containsExactly("WORKSHOP", "MEAL");
    assertThat(JsonPath.<List<String>>read(json, "$.consents[*].id"))
        .containsExactly("privacy", "photo");
    assertThat(JsonPath.<List<String>>read(json, "$.consents[*].text"))
        .containsExactly("I agree.", "Photos ok.");
    assertThat(JsonPath.<List<String>>read(json, "$.consents[*].givenAt"))
        .containsOnly("2026-10-12T10:15:30.123456Z");
    try (var files = Files.list(dir)) {
      assertThat(files.toList()).containsExactly(file);
    }
  }

  @Test
  void writesOnlyTheStudentFields() {
    byte[] written = store(temp).write(Registrations.student());

    Map<String, Object> root = JsonPath.read(new String(written, StandardCharsets.UTF_8), "$");
    assertThat(root)
        .containsEntry("studyInstitution", "Univerza")
        .containsEntry("studyProgramme", "CS")
        .containsEntry("studentId", "6320")
        .doesNotContainKey("organization");
    assertThat((List<?>) root.get("options")).isEmpty();
  }

  @Test
  void failsAndLeavesNoTemporaryFileWhenTheDirectoryCannotBeCreated() throws IOException {
    Path blocker = Files.writeString(temp.resolve("blocker"), "file");
    JsonCopyStore store = store(blocker.resolve("copies"));

    assertThatThrownBy(() -> store.write(Registrations.external()))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessage("JSON copy could not be written");
  }

  @Test
  void failsWhenTheTargetCannotBeReplaced() throws IOException {
    Path dir = Files.createDirectories(temp.resolve("copies"));
    Files.createDirectories(dir.resolve(Registrations.EXTERNAL_ID + ".json").resolve("child"));
    JsonCopyStore store = store(dir);

    assertThatThrownBy(() -> store.write(Registrations.external()))
        .isInstanceOf(UncheckedIOException.class);
    assertThat(dir.resolve(Registrations.EXTERNAL_ID + ".json.tmp")).doesNotExist();
  }

  @Test
  void deletesACopy() {
    JsonCopyStore store = store(temp);
    store.write(Registrations.external());

    store.delete(Registrations.EXTERNAL_ID);
    store.delete(Registrations.EXTERNAL_ID);

    assertThat(temp.resolve(Registrations.EXTERNAL_ID + ".json")).doesNotExist();
  }

  @Test
  void deleteIgnoresFilesThatCannotBeDeleted() throws IOException {
    Path dir = Files.createDirectories(temp.resolve("copies"));
    Path notEmpty = dir.resolve(Registrations.EXTERNAL_ID + ".json");
    Files.createDirectories(notEmpty.resolve("child"));

    store(dir).delete(Registrations.EXTERNAL_ID);

    assertThat(notEmpty).exists();
  }
}
