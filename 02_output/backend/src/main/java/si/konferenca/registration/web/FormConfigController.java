package si.konferenca.registration.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.RegistrationProperties;
import si.konferenca.registration.domain.ConferenceCatalog;

/** {@code GET /api/form-config} (US-003). */
@RestController
public class FormConfigController {

  private final FormConfigResponse response;

  public FormConfigController(ConferenceCatalog catalog, RegistrationProperties properties) {
    this.response =
        FormConfigResponse.of(
            properties.conferenceName(),
            properties.recaptcha().siteKey(),
            properties.recaptcha().testMode(),
            catalog);
  }

  @GetMapping("/api/form-config")
  public FormConfigResponse formConfig() {
    return response;
  }
}
