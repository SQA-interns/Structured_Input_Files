package si.konferenca.registration.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.RegistrationService;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.RegistrationForm;

/** {@code POST /api/registrations} (contract openapi.yaml). */
@RestController
public class RegistrationController {

  private final RegistrationService service;

  public RegistrationController(RegistrationService service) {
    this.service = service;
  }

  @PostMapping(path = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationAccepted register(
      @RequestBody RegistrationRequest body, HttpServletRequest request) {
    AcceptedRegistration accepted = service.register(body.toForm(), request.getRemoteAddr());
    return new RegistrationAccepted(
        accepted.reference().toString(),
        accepted.submittedAt().toString(),
        accepted.type().name(),
        accepted.participant().firstName(),
        accepted.participant().lastName(),
        accepted.options().stream()
            .map(o -> new AcceptedOption(o.id(), o.name(), o.category().name()))
            .toList());
  }

  /** Request body; every value is checked by the domain validator. */
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
      String captchaToken) {

    RegistrationForm toForm() {
      return new RegistrationForm(
          type,
          firstName,
          lastName,
          email,
          organization,
          studyInstitution,
          studyProgramme,
          studentId,
          optionIds,
          consentIds,
          captchaToken);
    }
  }

  /** Response body of an accepted registration (BR-06). */
  public record RegistrationAccepted(
      String reference,
      String submittedAt,
      String type,
      String firstName,
      String lastName,
      List<AcceptedOption> options) {}

  /** A selected option in the response. */
  public record AcceptedOption(String id, String name, String category) {}
}
