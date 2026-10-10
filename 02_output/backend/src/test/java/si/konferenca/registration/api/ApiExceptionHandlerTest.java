package si.konferenca.registration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import si.konferenca.registration.application.CaptchaRejectedException;
import si.konferenca.registration.application.DuplicateEmailException;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.ValidationException;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();
  private final HttpInputMessage input = new MockHttpInputMessage(new byte[0]);

  @Test
  void validationErrorsCarryFixedMessages() {
    ResponseEntity<ApiError> response =
        handler.invalid(
            new ValidationException(
                List.of(
                    new FieldError("email", "INVALID_EMAIL"), new FieldError("x", "SOMETHING"))));

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("VALIDATION_FAILED");
    assertThat(response.getBody().fieldErrors())
        .containsExactly(
            new ApiError.FieldErrorBody("email", "INVALID_EMAIL", "Enter a valid email address."),
            new ApiError.FieldErrorBody("x", "SOMETHING", "This value is not valid."));
  }

  @Test
  void captchaAndDuplicateBecomeFieldErrors() {
    ApiError captcha = handler.captcha(new CaptchaRejectedException()).getBody();
    ResponseEntity<ApiError> duplicate = handler.duplicate(new DuplicateEmailException());

    assertThat(captcha.status()).isEqualTo(400);
    assertThat(captcha.fieldErrors())
        .extracting(ApiError.FieldErrorBody::field)
        .containsExactly("captchaToken");
    assertThat(duplicate.getStatusCode().value()).isEqualTo(409);
    assertThat(duplicate.getBody().fieldErrors().get(0).message())
        .isEqualTo("A registration with this email already exists.");
  }

  @Test
  void oversizedBodyIs413OtherwiseUnreadableIs400() {
    HttpMessageNotReadableException tooLarge =
        new HttpMessageNotReadableException(
            "x", new IOException(new PayloadTooLargeException()), input);
    HttpMessageNotReadableException malformed = new HttpMessageNotReadableException("x", input);

    assertThat(handler.unreadable(tooLarge).getBody().error()).isEqualTo("PAYLOAD_TOO_LARGE");
    assertThat(handler.unreadable(tooLarge).getStatusCode().value()).isEqualTo(413);
    assertThat(handler.unreadable(malformed).getStatusCode().value()).isEqualTo(400);
  }

  @Test
  void unexpectedErrorsHideDetails() {
    ResponseEntity<ApiError> response =
        handler.unexpected(new IllegalStateException("secret detail"));

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody().message()).doesNotContain("secret");
    assertThat(handler.notFound(new IllegalArgumentException()).getStatusCode().value())
        .isEqualTo(404);
    assertThat(handler.mediaType(null).getStatusCode().value()).isEqualTo(415);
  }

  @Test
  void causeChainNamesTypesOnly() {
    String chain =
        ApiExceptionHandler.causeChain(
            new IllegalStateException("ana@example.si", new IOException("password")));

    assertThat(chain).isEqualTo("java.lang.IllegalStateException <- java.io.IOException");
  }
}
