package si.konferenca.registration.adapter.captcha;

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
import si.konferenca.registration.application.Ports.CaptchaVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Production verification against Google's siteverify endpoint (SR-01, spec §4 step 4). Anything
 * but an HTTP 200 answer with {@code "success": true} rejects the token.
 */
public class GoogleCaptchaVerifier implements CaptchaVerifier {

  private static final Logger LOG = LoggerFactory.getLogger(GoogleCaptchaVerifier.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final URI verifyUrl;
  private final String secretKey;
  private final HttpClient http;

  public GoogleCaptchaVerifier(URI verifyUrl, String secretKey) {
    this(verifyUrl, secretKey, HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
  }

  GoogleCaptchaVerifier(URI verifyUrl, String secretKey, HttpClient http) {
    this.verifyUrl = verifyUrl;
    this.secretKey = secretKey;
    this.http = http;
  }

  @Override
  public boolean verify(String token, String clientIp) {
    String form =
        "secret="
            + encode(secretKey)
            + "&response="
            + encode(token)
            + (clientIp == null ? "" : "&remoteip=" + encode(clientIp));
    HttpRequest request =
        HttpRequest.newBuilder(verifyUrl)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        LOG.warn("reCAPTCHA verification answered HTTP {}", response.statusCode());
        return false;
      }
      JsonNode success = JSON.readTree(response.body()).get("success");
      return success != null && success.isBoolean() && success.booleanValue();
    } catch (IOException | JacksonException e) {
      LOG.warn("reCAPTCHA verification failed ({})", e.getClass().getSimpleName());
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
