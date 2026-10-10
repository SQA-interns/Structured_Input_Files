package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestEnvironment;
import tools.jackson.databind.JsonNode;

/** US-005 Reliable registration storage (BR-07, AR-05). */
class RegistrationStorageAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_005_01_databaseRowAndJsonCopyExistWhenSuccessIsReturned() {
    String email = uniqueEmail("ac-005-01");
    Map<String, Object> body = validStudent(email);
    body.put("consentIds", List.of("data-processing", "photos"));

    JsonNode accepted = registerAccepted(body);

    String reference = accepted.path("reference").asString();
    Map<String, String> row = registrationRow(reference);
    assertThat(row).containsEntry("email", email).containsEntry("type", "STUDENT");
    Path copyFile = jsonCopy(TestEnvironment.JSON_DIR, reference);
    assertThat(copyFile).exists();
    JsonNode copy = JSON.readTree(readString(copyFile));
    assertThat(copy.path("schemaVersion").asInt()).isEqualTo(1);
    assertThat(copy.path("reference").asString()).isEqualTo(reference);
    assertThat(copy.path("type").asString()).isEqualTo("STUDENT");
    assertThat(Instant.parse(copy.path("submittedAt").asString()))
        .isEqualTo(Instant.parse(accepted.path("submittedAt").asString()));
    JsonNode participant = copy.path("participant");
    assertThat(participant.path("firstName").asString()).isEqualTo("Luka");
    assertThat(participant.path("lastName").asString()).isEqualTo("Kranjc");
    assertThat(participant.path("email").asString()).isEqualTo(email);
    assertThat(participant.path("studyInstitution").asString()).isEqualTo("Univerza v Mariboru");
    assertThat(participant.path("studyProgramme").asString())
        .isEqualTo("Računalništvo in informatika");
    assertThat(participant.path("studentId").asString()).isEqualTo("E1234567");
    assertThat(participant.has("organization")).isFalse();
    assertThat(copy.path("options").valueStream().map(o -> o.path("id").asString()))
        .containsExactly("ev-student-night", "meal-lunch");
    assertThat(copy.path("options").valueStream().map(o -> o.path("category").asString()))
        .containsExactly("EVENT", "MEAL");
    Set<String> consents =
        copy.path("consents")
            .valueStream()
            .peek(c -> assertThat(Instant.parse(c.path("givenAt").asString())).isNotNull())
            .map(c -> c.path("id").asString())
            .collect(Collectors.toSet());
    assertThat(consents).containsExactlyInAnyOrder("data-processing", "photos");
  }

  @Test
  void ac_005_02_failedJsonCopyMeansNoRowNoEmailAndAnErrorWithoutInternals() throws IOException {
    String email = uniqueEmail("ac-005-02");
    Path directory = TestEnvironment.JSON_DIR;
    Path moved = directory.resolveSibling(directory.getFileName() + "-moved");
    Files.move(directory, moved);
    Files.writeString(directory, "not a directory");
    HttpResponse<String> response;
    try {
      response = register(validExternal(email));
    } finally {
      Files.delete(directory);
      Files.move(moved, directory);
    }

    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(json(response).path("error").asString()).isEqualTo("INTERNAL_ERROR");
    assertNoInternals(response.body());
    assertThat(response.body()).doesNotContain(directory.toString());
    assertThat(registrationCountByEmail(email)).isZero();
    MAILPIT.assertNoMessageTo(email, Duration.ofSeconds(3));
  }
}
