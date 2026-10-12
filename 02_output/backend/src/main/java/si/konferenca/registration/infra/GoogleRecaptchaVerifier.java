package si.konferenca.registration.infra;

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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Production verifier: POST to the Google siteverify endpoint (specification 7.1). */
public class GoogleRecaptchaVerifier implements RecaptchaVerifier {

  private static final Logger LOG = LoggerFactory.getLogger(GoogleRecaptchaVerifier.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final URI verifyUrl;
  private final String secretKey;
  private final HttpClient http;

  public GoogleRecaptchaVerifier(String verifyUrl, String secretKey) {
    this(verifyUrl, secretKey, HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
  }

  GoogleRecaptchaVerifier(String verifyUrl, String secretKey, HttpClient http) {
    this.verifyUrl = URI.create(verifyUrl);
    this.secretKey = secretKey;
    this.http = http;
  }

  @Override
  public boolean verify(String token, String clientIp) {
    if (token == null || token.isBlank()) {
      return false;
    }
    String form = "secret=" + encode(secretKey) + "&response=" + encode(token);
    if (clientIp != null && !clientIp.isBlank()) {
      form += "&remoteip=" + encode(clientIp);
    }
    HttpRequest request =
        HttpRequest.newBuilder(verifyUrl)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        LOG.warn("reCAPTCHA verification answered status {}", response.statusCode());
        return false;
      }
      JsonNode answer = JSON.readTree(response.body());
      JsonNode success = answer == null ? null : answer.get("success");
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
