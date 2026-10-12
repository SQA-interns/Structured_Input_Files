package si.konferenca.registration.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.RegistrationService;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationForm;
import si.konferenca.registration.domain.RegistrationType;

/** {@code POST /api/registrations} (US-001, US-002; contract {@code openapi.yaml}). */
@RestController
class RegistrationController {

  record RegistrationRequest(
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      List<String> optionIds,
      Boolean consentGiven,
      String recaptchaToken) {

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
          consentGiven);
    }
  }

  record SelectedOptionResponse(String id, String name, Category category) {}

  record RegistrationConfirmation(
      UUID id,
      RegistrationType type,
      String firstName,
      String lastName,
      String email,
      List<SelectedOptionResponse> selectedOptions) {

    static RegistrationConfirmation of(Registration r) {
      return new RegistrationConfirmation(
          r.id(),
          r.type(),
          r.firstName(),
          r.lastName(),
          r.email(),
          r.selectedOptions().stream()
              .map(o -> new SelectedOptionResponse(o.id(), o.name(), o.category()))
              .toList());
    }
  }

  private final RegistrationService registrations;

  RegistrationController(RegistrationService registrations) {
    this.registrations = registrations;
  }

  @PostMapping(path = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RegistrationConfirmation register(
      @RequestBody RegistrationRequest request, HttpServletRequest http) {
    RegistrationService.Accepted accepted =
        registrations.register(request.toForm(), request.recaptchaToken(), http.getRemoteAddr());
    return RegistrationConfirmation.of(accepted.registration());
  }
}
