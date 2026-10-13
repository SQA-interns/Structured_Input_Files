package si.konferenca.registration.acceptance;

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
import tools.jackson.databind.JsonNode;

/** Real-HTTP client for the backend's public REST API (`openapi.yaml`). */
public final class Api {

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private final String baseUrl;

  public Api(int port) {
    this.baseUrl = "http://127.0.0.1:" + port;
  }

  /** An HTTP answer with its raw body. */
  public record Response(int status, HttpResponse<byte[]> raw) {

    public byte[] bytes() {
      return raw.body();
    }

    public String text() {
      return new String(raw.body(), StandardCharsets.UTF_8);
    }

    public JsonNode json() {
      return AcceptanceEnvironment.JSON.readTree(text());
    }

    public String header(String name) {
      return raw.headers().firstValue(name).orElse(null);
    }

    /** The field names of a 400 body's `errors` list. */
    public java.util.List<String> errorFields() {
      java.util.List<String> fields = new java.util.ArrayList<>();
      JsonNode errors = json().get("errors");
      if (errors != null) {
        errors.forEach(e -> fields.add(e.get("field").asString()));
      }
      return fields;
    }
  }

  public Response getFormConfig() {
    return send(HttpRequest.newBuilder(uri("/api/form-config")).GET());
  }

  public Response postRegistration(JsonNode body) {
    return postRegistration(AcceptanceEnvironment.JSON.writeValueAsString(body));
  }

  public Response postRegistration(String body) {
    return send(
        HttpRequest.newBuilder(uri("/api/registrations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)));
  }

  /** Export with optional Basic credentials (null = none) and extra request headers. */
  public Response export(String username, String password, Map<String, String> headers) {
    HttpRequest.Builder b = HttpRequest.newBuilder(uri("/api/registrations/export")).GET();
    if (username != null) {
      String token =
          Base64.getEncoder()
              .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
      b.header("Authorization", "Basic " + token);
    }
    headers.forEach(b::header);
    return send(b);
  }

  public Response exportAsOrganizer() {
    return export(
        AcceptanceEnvironment.ORGANIZER_USERNAME,
        AcceptanceEnvironment.ORGANIZER_PASSWORD,
        Map.of());
  }

  private URI uri(String path) {
    return URI.create(baseUrl + path);
  }

  private static Response send(HttpRequest.Builder builder) {
    try {
      HttpResponse<byte[]> r =
          HTTP.send(
              builder.timeout(Duration.ofSeconds(30)).build(),
              HttpResponse.BodyHandlers.ofByteArray());
      return new Response(r.statusCode(), r);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
