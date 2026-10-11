package si.konferenca.registration.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationService;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationType;

/** POST /api/registrations (openapi.json createRegistration). */
@RestController
public class RegistrationController {

  private final RegistrationService service;

  public RegistrationController(RegistrationService service) {
    this.service = service;
  }

  @PostMapping(path = "/api/registrations", consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationResponse register(
      @RequestBody RegistrationRequest body, HttpServletRequest request) {
    Registration r =
        service.register(
            new RegistrationSubmission(
                body.type(),
                body.firstName(),
                body.lastName(),
                body.email(),
                body.organization(),
                body.studyInstitution(),
                body.studyProgramme(),
                body.studentId(),
                body.optionIds(),
                body.consentIds(),
                body.captchaToken(),
                request.getRemoteAddr()));
    return new RegistrationResponse(
        r.id(),
        r.type(),
        r.participant().firstName(),
        r.participant().lastName(),
        r.options().stream()
            .map(o -> new SelectedOptionDto(o.id(), o.name(), o.category()))
            .toList());
  }

  /** Request body; unknown properties are rejected by Jackson configuration. */
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
      List<String> consentIds,
      String captchaToken) {}

  /** Response body (openapi.json RegistrationResponse). */
  public record RegistrationResponse(
      UUID id,
      RegistrationType type,
      String firstName,
      String lastName,
      List<SelectedOptionDto> options) {}

  /** A selected option in the response. */
  public record SelectedOptionDto(String id, String name, OptionCategory category) {}
}
