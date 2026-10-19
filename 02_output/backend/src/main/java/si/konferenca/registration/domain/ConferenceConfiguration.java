package si.konferenca.registration.domain;

import java.util.List;
import java.util.Optional;

/** The validated conference configuration: name, consents and options (AR-04). */
public final class ConferenceConfiguration {

  private ConferenceConfiguration() {}

  public static ConferenceConfiguration of(
      String conferenceName, List<ConsentDefinition> consents, List<ConferenceOption> options) {
    throw new UnsupportedOperationException("not implemented");
  }

  public String conferenceName() {
    throw new UnsupportedOperationException("not implemented");
  }

  public List<ConsentDefinition> consents() {
    throw new UnsupportedOperationException("not implemented");
  }

  public List<ConferenceOption> options() {
    throw new UnsupportedOperationException("not implemented");
  }

  public List<ConferenceOption> activeOptions() {
    throw new UnsupportedOperationException("not implemented");
  }

  public Optional<ConferenceOption> option(String id) {
    throw new UnsupportedOperationException("not implemented");
  }
}
