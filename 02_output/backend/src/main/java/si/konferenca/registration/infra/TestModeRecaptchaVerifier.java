package si.konferenca.registration.infra;

/**
 * Deterministic test mode (docs/02_contracts/recaptcha.json): exactly {@value #ACCEPTED_TOKEN} is
 * accepted, no network call is made. Enabled only by configuration (SR-02).
 */
public class TestModeRecaptchaVerifier implements RecaptchaVerifier {

  public static final String ACCEPTED_TOKEN = "test-pass";

  @Override
  public boolean verify(String token, String clientIp) {
    return ACCEPTED_TOKEN.equals(token);
  }
}
