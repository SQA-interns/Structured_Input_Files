package si.konferenca.registration.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.databind.JsonNode;

/** POST /api/registrations (openapi.json createRegistration). */
@RestController
public class RegistrationController {

  private static final Set<String> TEXT_PROPERTIES =
      Set.of(
          "type",
          "firstName",
          "lastName",
          "email",
          "organization",
          "studyInstitution",
          "studyProgramme",
          "studentId",
          "recaptchaToken");
  private static final Set<String> LIST_PROPERTIES = Set.of("optionIds", "consentIds");

  private final RegisterParticipant registerParticipant;

  public RegistrationController(RegisterParticipant registerParticipant) {
    this.registerParticipant = registerParticipant;
  }

  @PostMapping(path = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<RegistrationResponse> register(@RequestBody JsonNode body) {
    Registration registration = registerParticipant.register(toSubmission(body));
    return ResponseEntity.created(URI.create("/api/registrations/" + registration.id()))
        .body(RegistrationResponse.from(registration));
  }

  /** Accepts only the properties of openapi.json RegistrationRequest with their JSON types. */
  static RegistrationSubmission toSubmission(JsonNode body) {
    if (body == null || !body.isObject()) {
      throw new MalformedRequestException();
    }
    for (String name : body.propertyNames()) {
      if (!TEXT_PROPERTIES.contains(name) && !LIST_PROPERTIES.contains(name)) {
        throw new MalformedRequestException();
      }
    }
    return new RegistrationSubmission(
        text(body, "type"),
        text(body, "firstName"),
        text(body, "lastName"),
        text(body, "email"),
        text(body, "organization"),
        text(body, "studyInstitution"),
        text(body, "studyProgramme"),
        text(body, "studentId"),
        list(body, "optionIds"),
        list(body, "consentIds"),
        text(body, "recaptchaToken"));
  }

  private static String text(JsonNode body, String name) {
    JsonNode value = body.get(name);
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isString()) {
      throw new MalformedRequestException();
    }
    return value.asString();
  }

  private static List<String> list(JsonNode body, String name) {
    JsonNode value = body.get(name);
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isArray()) {
      throw new MalformedRequestException();
    }
    List<String> items = new ArrayList<>();
    for (JsonNode item : value) {
      if (!item.isString()) {
        throw new MalformedRequestException();
      }
      items.add(item.asString());
    }
    return items;
  }

  /** openapi.json RegistrationResponse. */
  public record RegistrationResponse(
      UUID id,
      RegistrationType type,
      String firstName,
      String lastName,
      List<OptionResponse> options,
      String registeredAt) {

    static RegistrationResponse from(Registration r) {
      return new RegistrationResponse(
          r.id(),
          r.type(),
          r.firstName(),
          r.lastName(),
          r.options().stream()
              .map(o -> new OptionResponse(o.id(), o.name(), o.category()))
              .toList(),
          r.registeredAt().toString());
    }
  }

  /** An option of RegistrationResponse. */
  public record OptionResponse(String id, String name, OptionCategory category) {}
}
