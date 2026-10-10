package si.konferenca.registration.domain;

import java.util.List;
import java.util.Set;

/** A small catalog for unit tests. */
public final class Catalogs {

  private Catalogs() {}

  public static OptionCatalog sample() {
    return new OptionCatalog(
        List.of(
            new ConferenceOption("ws-ai", "AI", OptionCategory.WORKSHOP, true, null),
            new ConferenceOption("ws-old", "Old", OptionCategory.WORKSHOP, false, null),
            new ConferenceOption(
                "ev-gala", "Gala", OptionCategory.EVENT, true, Set.of(RegistrationType.EXTERNAL)),
            new ConferenceOption(
                "ev-night", "Night", OptionCategory.EVENT, true, Set.of(RegistrationType.STUDENT)),
            new ConferenceOption("meal-lunch", "Lunch", OptionCategory.MEAL, true, Set.of())),
        List.of(
            new Consent("data", "Data processing", true), new Consent("photos", "Photos", false)));
  }
}
