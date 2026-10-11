package si.konferenca.registration.domain;

/** Public settings the registration form needs; the site key is the only key (AR-07). */
public record FormSettings(String conferenceName, boolean captchaTestMode, String captchaSiteKey) {}
