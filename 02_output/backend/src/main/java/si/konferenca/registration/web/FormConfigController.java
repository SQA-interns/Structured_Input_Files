package si.konferenca.registration.web;

import java.util.Map;
import si.konferenca.registration.application.FormConfigurationService;

/** GET /api/form-config (openapi.json getFormConfig). */
public class FormConfigController {

  public FormConfigController(FormConfigurationService service) {}

  public Map<String, Object> formConfig() {
    throw new UnsupportedOperationException("not implemented");
  }
}
