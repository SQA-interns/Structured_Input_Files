package si.konferenca.registration.infrastructure.captcha;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.application.CaptchaVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Production path (SR-01): verifies the token with the reCAPTCHA verify endpoint. */
public class RecaptchaVerifier implements CaptchaVerifier {

  private static final Logger LOG = LoggerFactory.getLogger(RecaptchaVerifier.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final URI verifyUrl;
  private final String secretKey;
  private final Duration timeout;
  private final HttpClient http;

  public RecaptchaVerifier(URI verifyUrl, String secretKey, Duration timeout) {
    this.verifyUrl = verifyUrl;
    this.secretKey = secretKey;
    this.timeout = timeout;
    this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  /** Fails closed: any error, timeout or unexpected answer rejects the token. */
  @Override
  public boolean verify(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    String form = "secret=" + encode(secretKey) + "&response=" + encode(token);
    HttpRequest request =
        HttpRequest.newBuilder(verifyUrl)
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        LOG.warn("reCAPTCHA verification answered status {}", response.statusCode());
        return false;
      }
      JsonNode success = JSON.readTree(response.body()).get("success");
      return success != null && success.isBoolean() && success.asBoolean();
    } catch (IOException | JacksonException e) {
      LOG.warn("reCAPTCHA verification failed: {}", e.getClass().getSimpleName());
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
