package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;

/** What the registration form needs: active options, consents and anti-automation settings. */
public record FormConfiguration(
    String conferenceName,
    List<ConferenceOption> activeOptions,
    List<ConsentDefinition> consents,
    boolean captchaTestMode,
    String siteKey) {}
