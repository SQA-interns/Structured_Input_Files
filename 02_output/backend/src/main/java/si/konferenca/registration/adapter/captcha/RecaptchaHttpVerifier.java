package si.konferenca.registration.adapter.captcha;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import si.konferenca.registration.domain.CaptchaUnavailableException;
import si.konferenca.registration.domain.CaptchaVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Google reCAPTCHA v2 server-side verification (contract recaptcha.md, SR-01). */
public final class RecaptchaHttpVerifier implements CaptchaVerifier {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(TIMEOUT).version(HttpClient.Version.HTTP_1_1).build();
  private final URI verifyUrl;
  private final String secretKey;

  public RecaptchaHttpVerifier(String verifyUrl, String secretKey) {
    this.verifyUrl = URI.create(verifyUrl);
    this.secretKey = secretKey;
  }

  @Override
  public boolean verify(String token, String clientAddress) {
    String form =
        "secret="
            + encode(secretKey)
            + "&response="
            + encode(token)
            + (clientAddress == null ? "" : "&remoteip=" + encode(clientAddress));
    HttpRequest request =
        HttpRequest.newBuilder(verifyUrl)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new CaptchaUnavailableException("verifier answered " + response.statusCode(), null);
      }
      Map<?, ?> body = JSON.readValue(response.body(), Map.class);
      return Boolean.TRUE.equals(body.get("success"));
    } catch (IOException | JacksonException e) {
      throw new CaptchaUnavailableException("verifier not reachable or answer unreadable", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CaptchaUnavailableException("interrupted", e);
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
