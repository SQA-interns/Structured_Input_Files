package si.konferenca.registration.application;

/**
 * Settings shown on the form: the conference name and the anti-automation mode with its public site
 * key (spec §5, AR-07).
 */
public record FormSettings(String conferenceName, boolean captchaTestMode, String siteKey) {}
