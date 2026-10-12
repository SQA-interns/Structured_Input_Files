package si.konferenca.registration.conference;

import java.util.List;
import java.util.Optional;

/** The validated, immutable conference configuration (options and consents). */
public final class ConferenceConfiguration {

  private final List<ConferenceOption> options;
  private final List<ConsentDefinition> consents;

  public ConferenceConfiguration(List<ConferenceOption> options, List<ConsentDefinition> consents) {
    this.options = List.copyOf(options);
    this.consents = List.copyOf(consents);
  }

  /** Active options in configuration order. */
  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  public Optional<ConferenceOption> option(String id) {
    return options.stream().filter(o -> o.id().equals(id)).findFirst();
  }

  public List<ConsentDefinition> consents() {
    return consents;
  }
}
