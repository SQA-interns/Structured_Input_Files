package si.konferenca.registration.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.service.RegistrationCommand;
import si.konferenca.registration.service.RegistrationService;

/** POST /api/registrations (US-001, US-002). */
@RestController
public class RegistrationController {

  private final RegistrationService service;

  public RegistrationController(RegistrationService service) {
    this.service = service;
  }

  /** Request body of openapi.json (RegistrationRequest). */
  public record RegistrationRequest(
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      List<String> optionIds,
      List<String> consents,
      String recaptchaToken) {}

  /** Response body of openapi.json (RegistrationCreated). */
  public record RegistrationCreated(
      String id,
      String type,
      String firstName,
      String lastName,
      List<OptionSummary> options,
      String registeredAt) {}

  /** A selected option in the response. */
  public record OptionSummary(String id, String name, String category) {}

  @PostMapping("/api/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationCreated register(
      @RequestBody RegistrationRequest body, HttpServletRequest request) {
    Registration registration =
        service.register(
            new RegistrationCommand(
                body.type(),
                body.firstName(),
                body.lastName(),
                body.email(),
                body.organization(),
                body.studyInstitution(),
                body.studyProgramme(),
                body.studentId(),
                body.optionIds(),
                body.consents(),
                body.recaptchaToken(),
                request.getRemoteAddr()));
    return new RegistrationCreated(
        registration.getId().toString(),
        registration.getType().name(),
        registration.getFirstName(),
        registration.getLastName(),
        registration.getOptions().stream()
            .map(o -> new OptionSummary(o.optionId(), o.optionName(), o.category()))
            .toList(),
        registration.getRegisteredAt().toString());
  }
}
