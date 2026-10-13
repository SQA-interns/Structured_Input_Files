package si.konferenca.registration.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The configured options and consents, in configuration order (US-003, AR-04). */
public final class ConferenceCatalogue {

  private final List<ConferenceOption> options;
  private final List<ConsentDefinition> consents;

  public ConferenceCatalogue(List<ConferenceOption> options, List<ConsentDefinition> consents) {
    this.options = List.copyOf(options);
    this.consents = List.copyOf(consents);
    requireUnique(this.options.stream().map(ConferenceOption::id).toList(), "option");
    requireUnique(this.consents.stream().map(ConsentDefinition::id).toList(), "consent");
  }

  private static void requireUnique(List<String> ids, String kind) {
    Set<String> seen = new HashSet<>();
    for (String id : ids) {
      if (!seen.add(id)) {
        throw new InvalidConfigurationException("Duplicate " + kind + " id '" + id + "'");
      }
    }
  }

  /** Every configured option, active or not. */
  public List<ConferenceOption> options() {
    return options;
  }

  /** The active options in configuration order (AC-003-01). */
  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  public List<ConsentDefinition> consents() {
    return consents;
  }

  public Optional<ConferenceOption> option(String id) {
    return options.stream().filter(o -> o.id().equals(id)).findFirst();
  }

  public Optional<ConsentDefinition> consent(String id) {
    return consents.stream().filter(c -> c.id().equals(id)).findFirst();
  }
}
