package si.konferenca.registration.domain;

import java.util.List;
import java.util.Optional;

/** The configured options and consents (AR-04), immutable after start. */
public record OptionCatalog(List<ConferenceOption> options, List<Consent> consents) {

  public OptionCatalog {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  /** Active options in configuration order. */
  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  public Optional<ConferenceOption> option(String id) {
    return options.stream().filter(o -> o.id().equals(id)).findFirst();
  }

  public Optional<Consent> consent(String id) {
    return consents.stream().filter(c -> c.id().equals(id)).findFirst();
  }
}
