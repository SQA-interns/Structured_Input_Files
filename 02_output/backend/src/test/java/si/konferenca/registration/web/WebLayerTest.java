package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.FieldErrors;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.export.RegistrationExport;
import si.konferenca.registration.service.RegistrationRejectedException;

class WebLayerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  private static void assertError(
      ResponseEntity<ErrorResponse> response, HttpStatus status, String message) {
    assertThat(response.getStatusCode()).isEqualTo(status);
    assertThat(response.getBody()).isEqualTo(new ErrorResponse(message, Map.of()));
  }

  @Test
  void errorModelForEveryFailure() {
    FieldErrors errors = new FieldErrors();
    errors.add("email", "Enter a valid email address.");
    ResponseEntity<ErrorResponse> rejected =
        handler.rejected(new RegistrationRejectedException(errors));
    assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(rejected.getBody())
        .isEqualTo(
            new ErrorResponse(
                "Please correct the marked fields.",
                Map.of("email", "Enter a valid email address.")));

    assertError(
        handler.unreadable(
            new HttpMessageNotReadableException("x", new MockHttpInputMessage(new byte[0]))),
        HttpStatus.BAD_REQUEST,
        "The request could not be read.");
    assertError(
        handler.unsupportedMediaType(new HttpMediaTypeNotSupportedException("text/plain")),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "The request could not be read.");
    assertError(
        handler.methodNotAllowed(new HttpRequestMethodNotSupportedException("PUT")),
        HttpStatus.METHOD_NOT_ALLOWED,
        "The request could not be read.");
    assertError(
        handler.notFound(new NoResourceFoundException(HttpMethod.GET, "/api/x", "x")),
        HttpStatus.NOT_FOUND,
        "Not found.");
    assertError(
        handler.unexpected(new IllegalStateException("SELECT secret FROM table")),
        HttpStatus.INTERNAL_SERVER_ERROR,
        "The registration could not be completed. Please try again later.");
  }

  @Test
  void errorResponseCopiesFieldErrors() {
    Map<String, String> fields = new HashMap<>(Map.of("a", "b"));
    ErrorResponse response = new ErrorResponse("m", fields);
    fields.put("c", "d");
    assertThat(response.fieldErrors()).containsExactly(Map.entry("a", "b"));
    assertThat(ErrorResponse.of("m").fieldErrors()).isEmpty();
  }

  @Test
  void formConfigListsActiveOptionsWithSortedTypesAndHidesSiteKeyInTestMode() {
    ConferenceCatalog catalog =
        new ConferenceCatalog(
            List.of(
                new ConferenceOption(
                    "a",
                    "A",
                    OptionCategory.EVENT,
                    true,
                    EnumSet.of(RegistrationType.STUDENT, RegistrationType.EXTERNAL)),
                new ConferenceOption(
                    "b", "B", OptionCategory.MEAL, false, EnumSet.of(RegistrationType.STUDENT))),
            List.of(new ConsentDefinition("privacy", "P", true)));

    FormConfigResponse live = FormConfigResponse.of("Konf", "site-key", false, catalog);
    FormConfigResponse test = FormConfigResponse.of("Konf", "site-key", true, catalog);

    assertThat(live.conferenceName()).isEqualTo("Konf");
    assertThat(live.recaptchaSiteKey()).isEqualTo("site-key");
    assertThat(live.recaptchaTestMode()).isFalse();
    assertThat(live.consents())
        .containsExactly(new FormConfigResponse.Consent("privacy", "P", true));
    assertThat(live.options())
        .containsExactly(
            new FormConfigResponse.Option(
                "a",
                "A",
                OptionCategory.EVENT,
                List.of(RegistrationType.EXTERNAL, RegistrationType.STUDENT)));
    assertThat(test.recaptchaSiteKey()).isEmpty();
    assertThat(test.recaptchaTestMode()).isTrue();
    assertThatThrownBy(() -> live.options().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void requestMapsToSubmissionWithCopiedLists() {
    List<String> options = new ArrayList<>(List.of("ws"));
    RegistrationRequest request =
        new RegistrationRequest(
            "STUDENT", "A", "B", "e@x.co", null, "U", "P", "1", options, List.of("privacy"), "tok");
    options.add("late");

    RegistrationSubmission submission = request.toSubmission();

    assertThat(submission)
        .isEqualTo(
            new RegistrationSubmission(
                "STUDENT",
                "A",
                "B",
                "e@x.co",
                null,
                "U",
                "P",
                "1",
                List.of("ws"),
                List.of("privacy")));
    assertThat(request.recaptchaToken()).isEqualTo("tok");
    RegistrationRequest empty =
        new RegistrationRequest(null, null, null, null, null, null, null, null, null, null, null);
    assertThat(empty.optionIds()).isNull();
    assertThat(empty.consents()).isNull();
  }

  @Test
  void exportIsAnAttachmentThatIsNotCached() {
    RegistrationExport export = mock(RegistrationExport.class);
    when(export.workbook()).thenReturn(new byte[] {80, 75});

    ResponseEntity<byte[]> response = new ExportController(export).registrations();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsExactly(80, 75);
    assertThat(response.getHeaders().getContentType())
        .hasToString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    assertThat(response.getHeaders().getFirst("Content-Disposition"))
        .isEqualTo("attachment; filename=\"registrations.xlsx\"");
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
  }
}
