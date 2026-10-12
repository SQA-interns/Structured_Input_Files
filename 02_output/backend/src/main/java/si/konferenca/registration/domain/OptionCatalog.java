package si.konferenca.registration.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The configured options and consent, in configuration order. */
public final class OptionCatalog {

  private final Consent consent;
  private final Map<String, ConferenceOption> options = new LinkedHashMap<>();

  public OptionCatalog(Consent consent, List<ConferenceOption> options) {
    this.consent = consent;
    for (ConferenceOption option : options) {
      if (this.options.putIfAbsent(option.id(), option) != null) {
        throw new IllegalArgumentException("Duplicate option id '" + option.id() + "'");
      }
    }
  }

  public Consent consent() {
    return consent;
  }

  public List<ConferenceOption> activeOptions() {
    return options.values().stream().filter(ConferenceOption::active).toList();
  }

  public Optional<ConferenceOption> find(String id) {
    return Optional.ofNullable(options.get(id));
  }
}
