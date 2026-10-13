package si.konferenca.registration.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import si.konferenca.registration.adapter.web.ApiResponses.FieldErrorBody;
import si.konferenca.registration.adapter.web.ApiResponses.GeneralError;
import si.konferenca.registration.adapter.web.ApiResponses.ValidationError;
import si.konferenca.registration.application.StorageFailedException;

/** Error bodies of `openapi.yaml` without internal details (SB-07). */
class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  private static HttpMessageNotReadableException unreadable(Throwable cause) {
    return new HttpMessageNotReadableException("bad", cause, new MockHttpInputMessage(new byte[0]));
  }

  @Test
  void malformedBodyIsAFieldErrorOnBody() {
    ResponseEntity<Object> response = handler.unreadable(unreadable(new IOException("x")));
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody())
        .isEqualTo(
            new ValidationError(
                "Validation failed", List.of(new FieldErrorBody("body", "Malformed request."))));
  }

  @Test
  void bodyAboveTheLimitWhileParsingIsTooLarge() {
    ResponseEntity<Object> response =
        handler.unreadable(unreadable(new IllegalStateException(new RequestTooLargeException())));
    assertThat(response.getStatusCode().value()).isEqualTo(413);
    assertThat(response.getBody()).isEqualTo(new GeneralError("Request too large."));
    assertThat(handler.tooLarge().getStatusCode().value()).isEqualTo(413);
  }

  @Test
  void storageFailureIsAServerErrorWithAGenericMessage() {
    ResponseEntity<Object> response =
        handler.storageFailed(new StorageFailedException("disk /data full", new IOException()));
    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody())
        .isEqualTo(new GeneralError("Registration could not be stored. Please try again later."));
  }

  @Test
  void otherErrorsMapToTheirStatusWithoutDetails() {
    assertThat(handler.unsupportedMediaType().getStatusCode().value()).isEqualTo(415);
    assertThat(handler.methodNotAllowed().getStatusCode().value()).isEqualTo(405);
    assertThat(handler.notFound().getStatusCode().value()).isEqualTo(404);
    ResponseEntity<Object> unexpected =
        handler.unexpected(new IllegalStateException("SELECT * FROM registration"));
    assertThat(unexpected.getStatusCode().value()).isEqualTo(500);
    assertThat(unexpected.getBody()).isEqualTo(new GeneralError("An unexpected error occurred."));
  }

  @Test
  void tooLargeIsFoundOnlyInTheCauseChain() {
    assertThat(RequestTooLargeException.isCause(new RequestTooLargeException())).isTrue();
    assertThat(RequestTooLargeException.isCause(new IOException(new RequestTooLargeException())))
        .isTrue();
    assertThat(RequestTooLargeException.isCause(new IOException("other"))).isFalse();
    assertThat(RequestTooLargeException.isCause(null)).isFalse();
  }

  @Test
  void jsonErrorsEscapeQuotesBackslashesAndControlCharacters() {
    assertThat(JsonErrors.escape("a\"b\\c\nd\u0001")).isEqualTo("a\\\"b\\\\c\\u000ad\\u0001");
    assertThat(JsonErrors.escape("čšž ok")).isEqualTo("čšž ok");
  }
}
