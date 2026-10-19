package si.konferenca.registration.application;

/** What the frontend needs to show the reCAPTCHA widget; never the secret key. */
public record CaptchaSettings(String siteKey, boolean testMode) {}
