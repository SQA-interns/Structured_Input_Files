package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import si.konferenca.registration.acceptance.support.AcceptanceEnvironment;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;

/** US-008: GET /api/registrations/export for organizers (excel-export.md). */
class ExportAcceptanceTest extends AcceptanceTestBase {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final List<String> HEADER =
      List.of(
          "Registration ID",
          "Type",
          "Registered at",
          "First name",
          "Last name",
          "Email",
          "Organization / institution",
          "Study institution",
          "Study programme",
          "Student ID",
          "Options",
          "Consents");

  @Test
  void AC_008_01_organizerGetsWorkbookWithOneRowPerRegistration() throws Exception {
    Map<String, Object> external = Submissions.validExternal();
    String externalId = acceptedId(external);
    Map<String, Object> student = Submissions.validStudent();
    String studentId = acceptedId(student);

    MvcResult result = mvc.perform(organizer(get("/api/registrations/export"))).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentType()).startsWith(XLSX);
    List<List<String>> rows = rows(result.getResponse().getContentAsByteArray());
    assertThat(rows).hasSize(3);
    assertThat(rows.get(0)).isEqualTo(HEADER);

    List<String> first = rows.get(1);
    assertThat(first.get(0)).isEqualTo(externalId);
    assertThat(first.get(1)).isEqualTo("EXTERNAL");
    assertThat(first.get(2)).isEqualTo(copiedRegisteredAt(externalId));
    assertThat(first.subList(3, 10))
        .containsExactly(
            "Ana", "Novak", (String) external.get("email"), "Institut Jozef Stefan", "", "", "");
    assertThat(first.get(10)).isEqualTo("ws-ai, ev-dinner, meal-lunch, oth-tour");
    assertThat(first.get(11)).startsWith("privacy (").endsWith(")");

    List<String> second = rows.get(2);
    assertThat(second.get(0)).isEqualTo(studentId);
    assertThat(second.get(1)).isEqualTo("STUDENT");
    assertThat(second.get(2)).isEqualTo(copiedRegisteredAt(studentId));
    assertThat(second.subList(3, 10))
        .containsExactly(
            "Luka",
            "Kranjc",
            (String) student.get("email"),
            "",
            "Univerza v Ljubljani",
            "Računalništvo in informatika",
            "63210001");
    assertThat(second.get(10)).isEqualTo("ws-ai, ev-dinner, meal-lunch, oth-mentoring");
    assertThat(second.get(11)).startsWith("privacy (").endsWith(")");
  }

  @Test
  void AC_008_02_exportWithoutOrWithWrongCredentialsIsRefusedWithoutData() throws Exception {
    acceptedId(Submissions.validExternal());

    MvcResult anonymous = mvc.perform(get("/api/registrations/export")).andReturn();
    MvcResult wrongPassword =
        mvc.perform(
                get("/api/registrations/export")
                    .header(
                        "Authorization",
                        basic(AcceptanceEnvironment.ORGANIZER_USERNAME, "wrong-password")))
            .andReturn();
    MvcResult wrongUser =
        mvc.perform(
                get("/api/registrations/export")
                    .header(
                        "Authorization",
                        basic("someone", AcceptanceEnvironment.ORGANIZER_PASSWORD)))
            .andReturn();

    for (MvcResult refused : List.of(anonymous, wrongPassword, wrongUser)) {
      assertThat(refused.getResponse().getStatus()).isEqualTo(401);
      assertThat(String.valueOf(refused.getResponse().getContentType())).doesNotStartWith(XLSX);
      assertThat(refused.getResponse().getContentAsString(StandardCharsets.UTF_8))
          .doesNotContain("Novak")
          .doesNotContain("Institut");
    }
  }

  @Test
  void AC_008_03_slovenianCharactersAreUnchangedInTheWorkbook() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("firstName", "Žiga Čedomir");
    body.put("lastName", "Šuštaršič-Žužek");
    body.put("organization", "Fakulteta za računalništvo, ČŠŽ čšž");
    acceptedId(body);

    MvcResult result = mvc.perform(organizer(get("/api/registrations/export"))).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    List<String> row = rows(result.getResponse().getContentAsByteArray()).get(1);
    assertThat(row.get(3)).isEqualTo("Žiga Čedomir");
    assertThat(row.get(4)).isEqualTo("Šuštaršič-Žužek");
    assertThat(row.get(6)).isEqualTo("Fakulteta za računalništvo, ČŠŽ čšž");
  }

  @Test
  void AC_008_04_validCredentialsOverPlainHttpFromRemoteClientAreRefused() throws Exception {
    acceptedId(Submissions.validExternal());

    MvcResult result =
        mvc.perform(
                organizer(get("/api/registrations/export"))
                    .secure(false)
                    .with(
                        request -> {
                          request.setRemoteAddr("203.0.113.10");
                          request.setRemoteHost("203.0.113.10");
                          return request;
                        }))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(403);
    assertThat(String.valueOf(result.getResponse().getContentType())).doesNotStartWith(XLSX);
    assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
        .doesNotContain("Novak")
        .doesNotContain("Institut");
  }

  private String acceptedId(Map<String, Object> body) throws Exception {
    MvcResult result = submit(body);
    assertThat(result.getResponse().getStatus()).as("registration accepted").isEqualTo(201);
    return json(result).path("id").asString();
  }

  private String copiedRegisteredAt(String id) throws Exception {
    return JSON.readTree(Files.readString(jsonCopyDir().resolve(id + ".json")))
        .path("registeredAt")
        .asString();
  }

  private static MockHttpServletRequestBuilder organizer(MockHttpServletRequestBuilder request) {
    return request.header(
        "Authorization",
        basic(AcceptanceEnvironment.ORGANIZER_USERNAME, AcceptanceEnvironment.ORGANIZER_PASSWORD));
  }

  private static String basic(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  /** Every row of sheet Registrations as 12 strings (empty for blank cells). */
  static List<List<String>> rows(byte[] xlsx) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = workbook.getSheet("Registrations");
      assertThat(sheet).as("sheet Registrations").isNotNull();
      DataFormatter formatter = new DataFormatter();
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (int c = 0; c < HEADER.size(); c++) {
          cells.add(row.getCell(c) == null ? "" : formatter.formatCellValue(row.getCell(c)));
        }
        rows.add(cells);
      }
      return rows;
    }
  }
}
