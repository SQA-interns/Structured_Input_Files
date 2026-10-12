package si.konferenca.registration.acceptance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** HTTP client for the public REST API (contract {@code openapi.yaml}). */
final class Api {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final String baseUrl;

  Api(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  Response get(String path) throws Exception {
    return send(request(path).GET().build());
  }

  Response get(String path, String username, String password) throws Exception {
    String token =
        Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    return send(request(path).header("Authorization", "Basic " + token).GET().build());
  }

  Response postRegistration(Map<String, Object> body) throws Exception {
    return postJson("/api/registrations", JSON.writeValueAsString(body));
  }

  Response postJson(String path, String json) throws Exception {
    return send(
        request(path)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build());
  }

  private HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(60));
  }

  private Response send(HttpRequest request) throws Exception {
    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    return new Response(response.statusCode(), response.body(), response.headers().map());
  }
}
