package si.konferenca.registration.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.service.RegistrationService;

/** {@code POST /api/registrations} (US-001, US-002). */
@RestController
public class RegistrationController {

  private final RegistrationService service;

  public RegistrationController(RegistrationService service) {
    this.service = service;
  }

  @PostMapping(path = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Created register(@RequestBody RegistrationRequest request, HttpServletRequest http) {
    Registration registration =
        service.register(request.toSubmission(), request.recaptchaToken(), http.getRemoteAddr());
    return new Created(registration.id(), registration.registeredAt());
  }

  /** OpenAPI schema RegistrationCreated. */
  public record Created(UUID id, Instant registeredAt) {}
}
