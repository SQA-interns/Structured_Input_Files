package si.konferenca.registration.application;

import si.konferenca.registration.domain.Consent;

/** Public settings the registration form needs; never a secret (AR-07). */
public record FormSettings(
    String conferenceName, Consent consent, boolean recaptchaTestMode, String recaptchaSiteKey) {}
