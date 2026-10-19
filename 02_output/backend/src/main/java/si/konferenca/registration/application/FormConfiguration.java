package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;

/** Content of GET /api/form-config. */
public record FormConfiguration(
    String conferenceName,
    List<ConferenceOption> options,
    List<ConsentDefinition> consents,
    CaptchaSettings captcha) {}
