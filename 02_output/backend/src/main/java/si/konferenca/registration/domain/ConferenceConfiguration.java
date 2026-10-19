package si.konferenca.registration.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The validated conference configuration: name, consents and options (AR-04). */
public final class ConferenceConfiguration {

  private final String conferenceName;
  private final List<ConsentDefinition> consents;
  private final List<ConferenceOption> options;

  private ConferenceConfiguration(
      String conferenceName, List<ConsentDefinition> consents, List<ConferenceOption> options) {
    this.conferenceName = conferenceName;
    this.consents = List.copyOf(consents);
    this.options = List.copyOf(options);
  }

  /** Validates and creates the configuration; any violation is an InvalidConfigurationException. */
  public static ConferenceConfiguration of(
      String conferenceName, List<ConsentDefinition> consents, List<ConferenceOption> options) {
    requireText(conferenceName, "conferenceName is missing");
    Set<String> consentIds = new HashSet<>();
    for (ConsentDefinition consent : consents) {
      requireText(consent.id(), "a consent has no id");
      requireText(consent.text(), "consent '" + consent.id() + "' has no text");
      if (!consentIds.add(consent.id())) {
        throw new InvalidConfigurationException("duplicate consent id '" + consent.id() + "'");
      }
    }
    Set<String> optionIds = new HashSet<>();
    for (ConferenceOption option : options) {
      requireText(option.id(), "an option has no id");
      requireText(option.name(), "option '" + option.id() + "' has no name");
      if (option.category() == null) {
        throw new InvalidConfigurationException("option '" + option.id() + "' has no category");
      }
      if (!optionIds.add(option.id())) {
        throw new InvalidConfigurationException("duplicate option id '" + option.id() + "'");
      }
    }
    return new ConferenceConfiguration(conferenceName, consents, options);
  }

  private static void requireText(String value, String problem) {
    if (value == null || value.isBlank()) {
      throw new InvalidConfigurationException(problem);
    }
  }

  public String conferenceName() {
    return conferenceName;
  }

  public List<ConsentDefinition> consents() {
    return consents;
  }

  public List<ConferenceOption> options() {
    return options;
  }

  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  public Optional<ConferenceOption> option(String id) {
    return options.stream().filter(o -> o.id().equals(id)).findFirst();
  }
}
