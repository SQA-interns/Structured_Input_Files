package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-008 Registration export: GET /api/export (organizer HTTP Basic, HTTPS-only by default). */
class RegistrationExportAcceptanceTest extends AcceptanceTestBase {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String REMOTE_CLIENT = "203.0.113.10";

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    // ORGANIZER_HTTPS_ONLY is not set: the default (on) applies.
    TestStack.register(registry);
  }

  @Test
  @DisplayName(
      "AC-008-01 organizer export is an xlsx workbook with one complete row per registration")
  void ac_008_01_exportContainsOneRowPerRegistration() throws Exception {
    Map<String, Object> external = validExternal();
    external.put("consents", new ArrayList<>(List.of("privacy", "photo")));
    String externalId = register(external);
    String studentId = register(validStudent());

    MvcResult result = mvc.perform(withOrganizerCredentials(exportRequest())).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentType()).startsWith(XLSX);
    List<Map<String, String>> rows = rows(result.getResponse().getContentAsByteArray());
    assertThat(rows).hasSize(2);

    Map<String, String> ext = rowWith(rows, externalId);
    assertThat(ext)
        .containsEntry("Type", "EXTERNAL")
        .containsEntry("First name", "Ana")
        .containsEntry("Last name", "Novak")
        .containsEntry("Email", "ana.novak@example.org")
        .containsEntry("Organization", "Institut Example");
    assertThat(ext.get("Options")).contains("Workshop: Software testing").contains("Lunch");
    assertThat(ext.get("Consents")).contains("privacy").contains("photo");
    assertThat(OffsetDateTime.parse(ext.get("Registered at"))).isNotNull();

    Map<String, String> stu = rowWith(rows, studentId);
    assertThat(stu)
        .containsEntry("Type", "STUDENT")
        .containsEntry("First name", "Luka")
        .containsEntry("Last name", "Kranjc")
        .containsEntry("Email", "luka.kranjc@student.example.org")
        .containsEntry("Study institution", "Univerza Example")
        .containsEntry("Study programme", "Computer Science")
        .containsEntry("Student ID", "63200001");
    assertThat(stu.get("Options"))
        .contains("Workshop: Software testing")
        .contains("Opening reception");
    assertThat(stu.get("Consents")).contains("privacy");
    assertThat(OffsetDateTime.parse(stu.get("Registered at"))).isNotNull();
  }

  @Test
  @DisplayName("AC-008-01 two registrations with the same email are two rows")
  void ac_008_01_sameEmailTwiceGivesTwoRows() throws Exception {
    register(validExternal());
    register(validExternal());

    MvcResult result = mvc.perform(withOrganizerCredentials(exportRequest())).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(rows(result.getResponse().getContentAsByteArray())).hasSize(2);
  }

  @Test
  @DisplayName("AC-008-02 export without credentials is refused with 401 and no data")
  void ac_008_02_exportWithoutCredentialsIsRefused() throws Exception {
    register(validExternal());

    assertRefusedWith401(mvc.perform(exportRequest()).andReturn());
  }

  @Test
  @DisplayName("AC-008-02 export with a wrong password is refused with 401 and no data")
  void ac_008_02_exportWithWrongPasswordIsRefused() throws Exception {
    register(validExternal());

    assertRefusedWith401(
        mvc.perform(
                exportRequest()
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        basic(TestStack.ORGANIZER_USERNAME, "wrong-password-0000")))
            .andReturn());
  }

  @Test
  @DisplayName("AC-008-02 export with an unknown user is refused with 401 and no data")
  void ac_008_02_exportWithUnknownUserIsRefused() throws Exception {
    register(validExternal());

    assertRefusedWith401(
        mvc.perform(
                exportRequest()
                    .header(
                        HttpHeaders.AUTHORIZATION, basic("intruder", TestStack.ORGANIZER_PASSWORD)))
            .andReturn());
  }

  @Test
  @DisplayName("AC-008-03 Slovenian characters are unchanged in the workbook cells")
  void ac_008_03_slovenianCharactersAreUnchangedInWorkbook() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("firstName", "Čedomir Žiga");
    body.put("lastName", "Šuštar čšž");
    body.put("organization", "Fakulteta ČŠŽ čšž");
    String id = register(body);

    MvcResult result = mvc.perform(withOrganizerCredentials(exportRequest())).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    Map<String, String> row = rowWith(rows(result.getResponse().getContentAsByteArray()), id);
    assertThat(row)
        .containsEntry("First name", "Čedomir Žiga")
        .containsEntry("Last name", "Šuštar čšž")
        .containsEntry("Organization", "Fakulteta ČŠŽ čšž");
  }

  @Test
  @DisplayName("AC-008-04 valid credentials over plain HTTP from a non-local client are refused")
  void ac_008_04_plainHttpFromRemoteClientIsRefused() throws Exception {
    register(validExternal());

    MvcResult result =
        mvc.perform(withOrganizerCredentials(fromRemote(exportRequest()).secure(false)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isBetween(400, 499);
    assertNoRegistrationData(result);
  }

  @Test
  @DisplayName("AC-008-04 the same request over HTTPS from a non-local client is served")
  void ac_008_04_httpsFromRemoteClientIsServed() throws Exception {
    register(validExternal());

    MvcResult result =
        mvc.perform(withOrganizerCredentials(fromRemote(exportRequest()).secure(true))).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(rows(result.getResponse().getContentAsByteArray())).hasSize(1);
  }

  @Test
  @DisplayName("AC-008-04 plain HTTP from localhost is served")
  void ac_008_04_plainHttpFromLocalhostIsServed() throws Exception {
    register(validExternal());

    MvcResult result =
        mvc.perform(
                withOrganizerCredentials(
                    exportRequest()
                        .secure(false)
                        .with(
                            request -> {
                              request.setRemoteAddr("127.0.0.1");
                              return request;
                            })))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
  }

  // ---- helpers ------------------------------------------------------------------------------

  private static MockHttpServletRequestBuilder fromRemote(MockHttpServletRequestBuilder request) {
    return request.with(
        r -> {
          r.setRemoteAddr(REMOTE_CLIENT);
          r.setRemoteHost(REMOTE_CLIENT);
          return r;
        });
  }

  private void assertRefusedWith401(MvcResult result) throws Exception {
    assertThat(result.getResponse().getStatus()).isEqualTo(401);
    assertNoRegistrationData(result);
  }

  private static void assertNoRegistrationData(MvcResult result) throws Exception {
    String contentType = result.getResponse().getContentType();
    assertThat(contentType == null || !contentType.startsWith(XLSX)).isTrue();
    String text = body(result);
    assertThat(text).doesNotContain("Ana").doesNotContain("ana.novak@example.org");
    byte[] bytes = result.getResponse().getContentAsByteArray();
    boolean zip = bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K';
    assertThat(zip).as("response is not a workbook").isFalse();
  }

  /** Data rows of the first sheet as column header to cell text. */
  private static List<Map<String, String>> rows(byte[] xlsx) throws Exception {
    DataFormatter formatter = new DataFormatter();
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row header = sheet.getRow(0);
      List<String> columns = new ArrayList<>();
      for (int c = 0; c < header.getLastCellNum(); c++) {
        columns.add(formatter.formatCellValue(header.getCell(c)));
      }
      assertThat(columns)
          .contains(
              "Registration ID",
              "Type",
              "First name",
              "Last name",
              "Email",
              "Organization",
              "Study institution",
              "Study programme",
              "Student ID",
              "Options",
              "Consents",
              "Registered at");
      List<Map<String, String>> rows = new ArrayList<>();
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        Map<String, String> values = new HashMap<>();
        for (int c = 0; c < columns.size(); c++) {
          values.put(columns.get(c), formatter.formatCellValue(row.getCell(c)));
        }
        rows.add(values);
      }
      return rows;
    }
  }

  private static Map<String, String> rowWith(List<Map<String, String>> rows, String id) {
    List<Map<String, String>> found =
        rows.stream().filter(r -> id.equals(r.get("Registration ID"))).toList();
    assertThat(found).as("rows for %s", id).hasSize(1);
    return found.getFirst();
  }
}
