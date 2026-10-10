package si.konferenca.registration.infrastructure;

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

/**
 * Production reCAPTCHA v2 verification against the siteverify endpoint (contract
 * recaptcha-siteverify.openapi.yaml). Accepts only {@code "success": true}.
 */
public class GoogleRecaptchaVerifier implements CaptchaVerifier {

  private static final Logger LOG = LoggerFactory.getLogger(GoogleRecaptchaVerifier.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final URI verifyUrl;
  private final String secret;
  private final HttpClient http;
  private final JsonMapper json = JsonMapper.builder().build();

  public GoogleRecaptchaVerifier(String verifyUrl, String secret) {
    this.verifyUrl = URI.create(verifyUrl);
    this.secret = secret;
    this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
  }

  @Override
  public boolean verify(String token, String clientIp) {
    String body =
        "secret="
            + encode(secret)
            + "&response="
            + encode(token)
            + (clientIp == null ? "" : "&remoteip=" + encode(clientIp));
    HttpRequest request =
        HttpRequest.newBuilder(verifyUrl)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        LOG.warn("reCAPTCHA verification answered status {}", response.statusCode());
        return false;
      }
      JsonNode result = json.readTree(response.body());
      JsonNode success = result == null ? null : result.get("success");
      return success != null && success.isBoolean() && success.booleanValue();
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
