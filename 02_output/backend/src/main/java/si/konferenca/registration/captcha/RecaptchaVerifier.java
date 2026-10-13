package si.konferenca.registration.captcha;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import si.konferenca.registration.RegistrationProperties;
import si.konferenca.registration.service.CaptchaVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Google reCAPTCHA v2 siteverify, or the deterministic test mode (spec section 9). */
@Component
public class RecaptchaVerifier implements CaptchaVerifier {

  /** The only token test mode accepts. */
  public static final String TEST_MODE_TOKEN = "test-mode-token";

  private static final Logger LOG = LoggerFactory.getLogger(RecaptchaVerifier.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final RegistrationProperties.Recaptcha settings;
  private final HttpClient http;
  private final JsonMapper json = JsonMapper.builder().build();

  public RecaptchaVerifier(RegistrationProperties properties) {
    this.settings = properties.recaptcha();
    this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
  }

  @Override
  public boolean verify(String token, String clientIp) {
    if (settings.testMode()) {
      return TEST_MODE_TOKEN.equals(token);
    }
    String form =
        "secret="
            + encode(settings.secretKey())
            + "&response="
            + encode(token)
            + (clientIp == null ? "" : "&remoteip=" + encode(clientIp));
    HttpRequest request =
        HttpRequest.newBuilder(settings.verifyUrl())
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return false;
      }
      JsonNode success = json.readTree(response.body()).path("success");
      return success.isBoolean() && success.booleanValue();
    } catch (IOException | JacksonException e) {
      LOG.warn("reCAPTCHA verification failed: {}", e.getClass().getSimpleName());
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
