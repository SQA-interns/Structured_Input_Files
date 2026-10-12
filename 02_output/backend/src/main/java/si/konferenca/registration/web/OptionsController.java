package si.konferenca.registration.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.RegistrationType;

/** {@code GET /api/options}: active options only (AC-003-01). */
@RestController
class OptionsController {

  record OptionResponse(
      String id, String name, Category category, List<RegistrationType> registrationTypes) {

    static OptionResponse of(ConferenceOption option) {
      return new OptionResponse(
          option.id(), option.name(), option.category(), option.types().stream().sorted().toList());
    }
  }

  private final OptionCatalog catalog;

  OptionsController(OptionCatalog catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/api/options")
  List<OptionResponse> options() {
    return catalog.activeOptions().stream().map(OptionResponse::of).toList();
  }
}
