package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** US-008 Registration export (BR-08, SR-06, SR-07). */
class RegistrationExportAcceptanceTest extends AcceptanceTestBase {

  private static final List<String> COLUMNS =
      List.of(
          "Reference",
          "Submitted at",
          "Type",
          "First name",
          "Last name",
          "Email",
          "Organization / institution",
          "Study institution",
          "Study programme",
          "Student ID",
          "Workshops",
          "Events",
          "Meals",
          "Other activities",
          "Consents");

  @Test
  void ac_008_01_organizerDownloadsWorkbookWithOneRowPerRegistration() {
    String externalEmail = uniqueEmail("ac-008-01-ext");
    String studentEmail = uniqueEmail("ac-008-01-stu");
    String external = registerAccepted(validExternal(externalEmail)).path("reference").asString();
    String student = registerAccepted(validStudent(studentEmail)).path("reference").asString();

    HttpResponse<byte[]> response = export(organizerHeaders(randomClientIp()));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith(XLSX);
    assertThat(response.headers().firstValue("Content-Disposition").orElse(""))
        .contains("attachment")
        .contains("registrations.xlsx");
    List<List<String>> rows = readWorkbook(response.body());
    assertThat(rows.get(0)).containsExactlyElementsOf(COLUMNS);
    assertThat(rows.subList(1, rows.size())).hasSize((int) registrationCount());
    assertThat(rows.stream().filter(r -> r.contains(external))).hasSize(1);
    assertThat(rows.stream().filter(r -> r.contains(student))).hasSize(1);
    Map<String, String> externalRow = exportRow(rows, external);
    assertThat(externalRow)
        .containsEntry("Type", "EXTERNAL")
        .containsEntry("Email", externalEmail)
        .containsEntry("Organization / institution", "Institut Jožef Stefan")
        .containsEntry("Study institution", "")
        .containsEntry("Workshops", "Delavnica: umetna inteligenca")
        .containsEntry("Meals", "Kosilo");
    assertThat(externalRow.get("Submitted at")).isNotBlank();
    assertThat(externalRow.get("Consents")).contains("data-processing");
    assertThat(exportRow(rows, student))
        .containsEntry("Type", "STUDENT")
        .containsEntry("Student ID", "E1234567")
        .containsEntry("Organization / institution", "");
  }

  @Test
  void ac_008_02_missingCredentialsAreRefused() {
    Map<String, String> headers =
        Map.of("X-Forwarded-For", randomClientIp(), "X-Forwarded-Proto", "https");

    HttpResponse<byte[]> response = export(headers);

    assertRefused(response, 401);
    assertThat(response.headers().firstValue("WWW-Authenticate").orElse("")).startsWith("Basic");
  }

  @Test
  void ac_008_02_wrongPasswordIsRefused() {
    Map<String, String> headers =
        Map.of(
            "Authorization", basic(TestEnvironment.ORGANIZER_USERNAME, "wrong-password-000000"),
            "X-Forwarded-For", randomClientIp(),
            "X-Forwarded-Proto", "https");

    assertRefused(export(headers), 401);
  }

  @Test
  void ac_008_02_wrongUsernameIsRefused() {
    Map<String, String> headers =
        Map.of(
            "Authorization", basic("someone-else", TestEnvironment.ORGANIZER_PASSWORD),
            "X-Forwarded-For", randomClientIp(),
            "X-Forwarded-Proto", "https");

    assertRefused(export(headers), 401);
  }

  @Test
  void ac_008_03_plainHttpFromRemoteClientIsRefused() {
    Map<String, String> headers =
        Map.of(
            "Authorization",
            basic(TestEnvironment.ORGANIZER_USERNAME, TestEnvironment.ORGANIZER_PASSWORD),
            "X-Forwarded-For",
            randomClientIp());

    HttpResponse<byte[]> response = export(headers);

    assertRefused(response, 403);
    assertThat(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8))
        .contains("HTTPS_REQUIRED");
  }

  @Test
  void ac_008_05_exportContainsOnlyRegistrationData() {
    String reference =
        registerAccepted(validExternal(uniqueEmail("ac-008-05"))).path("reference").asString();
    String internalId = registrationRow(reference).get("id");

    HttpResponse<byte[]> response = export(organizerHeaders(randomClientIp()));

    assertThat(response.statusCode()).isEqualTo(200);
    List<List<String>> rows = readWorkbook(response.body());
    assertThat(rows.get(0)).containsExactlyElementsOf(COLUMNS);
    Map<String, String> row = exportRow(rows, reference);
    assertThat(row.values()).doesNotContain(internalId);
    assertThat(String.join("|", row.values()))
        .doesNotContain("jdbc:")
        .doesNotContain(TestEnvironment.ORGANIZER_USERNAME)
        .doesNotContain("si.konferenca");
  }

  private static void assertRefused(HttpResponse<byte[]> response, int status) {
    assertThat(response.statusCode()).isEqualTo(status);
    assertThat(response.headers().firstValue("Content-Type").orElse("")).doesNotContain(XLSX);
    assertThat(response.body()).doesNotContain(new byte[] {'P', 'K', 3, 4});
  }
}
