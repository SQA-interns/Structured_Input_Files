package si.konferenca.registration.acceptance.support;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** HTTP client for the backend's public interface (openapi.json). */
public final class Api {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final HttpClient client =
      HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .connectTimeout(Duration.ofSeconds(10))
          .build();
  private final String baseUrl;

  public Api(int port) {
    this.baseUrl = "http://127.0.0.1:" + port;
  }

  public Response get(String path, String... headers) {
    return send(request(path, headers).GET().build());
  }

  public Response getAsOrganizer(String path, String user, String password, String... headers) {
    String token =
        Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    HttpRequest.Builder b = request(path, headers).header("Authorization", "Basic " + token);
    return send(b.GET().build());
  }

  public Response postJson(String path, Map<String, Object> body, String... headers) {
    return postRaw(path, JSON.writeValueAsString(body), headers);
  }

  public Response postRaw(String path, String body, String... headers) {
    HttpRequest.Builder b =
        request(path, headers)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    return send(b.build());
  }

  public Response register(Map<String, Object> body) {
    return postJson("/api/registrations", body);
  }

  private HttpRequest.Builder request(String path, String... headers) {
    HttpRequest.Builder b =
        HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(30));
    for (int i = 0; i + 1 < headers.length; i += 2) {
      b.header(headers[i], headers[i + 1]);
    }
    return b;
  }

  private Response send(HttpRequest request) {
    try {
      HttpResponse<byte[]> r = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      return new Response(r.statusCode(), r.headers().map(), r.body());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  /** A received response. */
  public record Response(int status, Map<String, java.util.List<String>> headers, byte[] body) {

    public String text() {
      return new String(body, StandardCharsets.UTF_8);
    }

    public DocumentContext json() {
      return JsonPath.parse(text());
    }

    public String header(String name) {
      return headers.entrySet().stream()
          .filter(e -> e.getKey().equalsIgnoreCase(name))
          .map(e -> String.join(", ", e.getValue()))
          .findFirst()
          .orElse(null);
    }

    /** Field names listed in the error body's fieldErrors. */
    public java.util.List<String> fieldErrorFields() {
      return json().read("$.fieldErrors[*].field");
    }
  }
}
