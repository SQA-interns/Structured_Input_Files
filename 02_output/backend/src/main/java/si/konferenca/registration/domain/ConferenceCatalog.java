package si.konferenca.registration.domain;

import java.util.List;
import java.util.Optional;

/** The options and consents of the conference, as configured (AR-04). */
public record ConferenceCatalog(List<ConferenceOption> options, List<ConsentDefinition> consents) {

  public ConferenceCatalog {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  public Optional<ConferenceOption> option(String id) {
    return options.stream().filter(o -> o.id().equals(id)).findFirst();
  }

  public Optional<ConsentDefinition> consent(String id) {
    return consents.stream().filter(c -> c.id().equals(id)).findFirst();
  }
}
