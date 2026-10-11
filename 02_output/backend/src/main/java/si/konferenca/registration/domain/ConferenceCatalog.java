package si.konferenca.registration.domain;

import java.util.List;
import java.util.Optional;

/** Port: the configured options and consents (AR-04). */
public interface ConferenceCatalog {

  /** All configured options in configuration order, active or not. */
  List<ConferenceOption> options();

  List<ConsentDefinition> consents();

  default List<ConferenceOption> activeOptions() {
    return options().stream().filter(ConferenceOption::active).toList();
  }

  default Optional<ConferenceOption> option(String id) {
    return options().stream().filter(o -> o.id().equals(id)).findFirst();
  }
}
