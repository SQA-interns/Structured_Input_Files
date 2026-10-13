package si.konferenca.registration.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.adapter.web.ApiResponses.FieldErrorBody;
import si.konferenca.registration.adapter.web.ApiResponses.ValidationError;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.application.RegisterParticipant.Accepted;
import si.konferenca.registration.application.RegisterParticipant.Outcome;
import si.konferenca.registration.application.RegisterParticipant.Rejected;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.Submission;

/** `POST /api/registrations` (US-001, US-002, spec §4). */
@RestController
public class RegistrationController {

  private final RegisterParticipant registerParticipant;

  public RegistrationController(RegisterParticipant registerParticipant) {
    this.registerParticipant = registerParticipant;
  }

  /** `RegistrationRequest`; unknown properties are rejected by the JSON mapper. */
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
      Map<String, Boolean> consents,
      String recaptchaToken) {

    Submission toSubmission() {
      return new Submission(
          type,
          firstName,
          lastName,
          email,
          organization,
          studyInstitution,
          studyProgramme,
          studentId,
          optionIds,
          consents,
          recaptchaToken);
    }
  }

  /** `RegistrationAccepted`. */
  public record RegistrationAccepted(
      String id,
      String type,
      String firstName,
      String lastName,
      List<AcceptedOption> options,
      String registeredAt) {}

  /** An option of `RegistrationAccepted`. */
  public record AcceptedOption(String id, String name, String category) {}

  @PostMapping(path = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> register(
      @RequestBody RegistrationRequest body, HttpServletRequest request) {
    Outcome outcome = registerParticipant.register(body.toSubmission(), request.getRemoteAddr());
    if (outcome instanceof Rejected rejected) {
      return ResponseEntity.badRequest()
          .body(
              ValidationError.of(
                  rejected.errors().stream()
                      .map(e -> new FieldErrorBody(e.field(), e.message()))
                      .toList()));
    }
    Registration r = ((Accepted) outcome).registration();
    return ResponseEntity.status(HttpStatus.CREATED)
        .location(URI.create("/api/registrations/" + r.id()))
        .body(
            new RegistrationAccepted(
                r.id().toString(),
                r.type().name(),
                r.firstName(),
                r.lastName(),
                r.options().stream()
                    .map(o -> new AcceptedOption(o.id(), o.name(), o.category().name()))
                    .toList(),
                Registration.format(r.registeredAt())));
  }
}
