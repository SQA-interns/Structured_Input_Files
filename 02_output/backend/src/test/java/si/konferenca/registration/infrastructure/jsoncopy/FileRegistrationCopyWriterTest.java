package si.konferenca.registration.infrastructure.jsoncopy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.infrastructure.TestRegistrations;

class FileRegistrationCopyWriterTest {

  @TempDir Path dir;

  @Test
  void writesTheCopyAtomicallyOwnerOnlyAndReturnsTheSameBytes() throws Exception {
    FileRegistrationCopyWriter writer = new FileRegistrationCopyWriter(dir);

    byte[] bytes = writer.write(TestRegistrations.student());

    Path file = dir.resolve("22222222-2222-2222-2222-222222222222.json");
    assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
    assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(file)))
        .isEqualTo("rw-------");
    try (var files = Files.list(dir)) {
      assertThat(files).containsExactly(file);
    }
    String json = new String(bytes, StandardCharsets.UTF_8);
    assertThat(json).startsWith("{\"id\":\"22222222");
    assertThat(json).doesNotContain("organization").doesNotContain("null");
    assertThat((String) JsonPath.read(json, "$.submittedAt")).isEqualTo("2026-10-12T10:01:00Z");
    assertThat((List<String>) JsonPath.read(json, "$.selectedOptions[*].category"))
        .containsExactly("OTHER", "WORKSHOP", "WORKSHOP");
    assertThat((String) JsonPath.read(json, "$.consent.givenAt")).isEqualTo("2026-10-12T10:01:00Z");
  }

  @Test
  void externalCopyHasTheOrganizationButNoStudentFields() {
    String json =
        new String(
            new FileRegistrationCopyWriter(dir)
                .write(TestRegistrations.external("Žiga", List.of())),
            StandardCharsets.UTF_8);

    assertThat(json).contains("\"organization\":\"Acme\"").contains("Žiga");
    assertThat(json).doesNotContain("studentId").doesNotContain("studyInstitution");
  }

  @Test
  void deleteRemovesTheCopyAndIgnoresAMissingOne() {
    FileRegistrationCopyWriter writer = new FileRegistrationCopyWriter(dir);
    writer.write(TestRegistrations.student());

    writer.delete(TestRegistrations.student().id());
    writer.delete(TestRegistrations.student().id());

    assertThat(dir.resolve("22222222-2222-2222-2222-222222222222.json")).doesNotExist();
  }

  @Test
  void createsAMissingDirectoryAndFailsWhenItCannotWrite() throws Exception {
    Path nested = dir.resolve("a/b");
    new FileRegistrationCopyWriter(nested).write(TestRegistrations.student());
    assertThat(nested).isDirectory();

    Path readOnly = Files.createDirectory(dir.resolve("ro"));
    Files.setPosixFilePermissions(readOnly, PosixFilePermissions.fromString("r-x------"));
    try {
      assertThatThrownBy(
              () -> new FileRegistrationCopyWriter(readOnly).write(TestRegistrations.student()))
          .isInstanceOf(UncheckedIOException.class);
    } finally {
      Files.setPosixFilePermissions(readOnly, PosixFilePermissions.fromString("rwx------"));
    }
  }
}
