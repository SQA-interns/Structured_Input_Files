package si.konferenca.registration.infrastructure.captcha;

import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import si.konferenca.registration.application.CaptchaVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Live reCAPTCHA v2 verification against the siteverify URL (SR-01, anti-automation.md). */
public class GoogleCaptchaVerifier implements CaptchaVerifier {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final RestClient client;
  private final String verifyUrl;
  private final String secret;

  public GoogleCaptchaVerifier(String verifyUrl, String secret) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(5));
    this.client = RestClient.builder().requestFactory(factory).build();
    this.verifyUrl = verifyUrl;
    this.secret = secret;
  }

  @Override
  public boolean verify(String token, String clientIp) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("secret", secret);
    form.add("response", token);
    if (clientIp != null) {
      form.add("remoteip", clientIp);
    }
    try {
      String body =
          client
              .post()
              .uri(verifyUrl)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(String.class);
      if (body == null) {
        return false;
      }
      JsonNode success = JSON.readTree(body).get("success");
      return success != null && success.isBoolean() && success.booleanValue();
    } catch (RestClientException | JacksonException e) {
      return false;
    }
  }
}
