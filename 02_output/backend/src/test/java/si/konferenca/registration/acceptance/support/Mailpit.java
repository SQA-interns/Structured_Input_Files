package si.konferenca.registration.acceptance.support;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Client for the Mailpit HTTP API (the local SMTP substitute). */
public final class Mailpit {

  private final String baseUrl;
  private final HttpClient http = HttpClient.newHttpClient();

  Mailpit(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** A received message: summary fields from the API plus the plain-text and HTML bodies. */
  public record Message(
      String id,
      String subject,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String text,
      String html,
      List<Map<String, Object>> attachments) {}

  public void deleteAll() {
    send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/messages")).DELETE().build());
  }

  public int total() {
    String json = get("/api/v1/messages?limit=1");
    return ((Number) JsonPath.read(json, "$.messages_count")).intValue();
  }

  /** All messages, newest first, with full bodies. */
  public List<Message> all() {
    String json = get("/api/v1/messages?limit=500");
    List<String> ids = JsonPath.read(json, "$.messages[*].ID");
    return ids.stream().map(this::message).toList();
  }

  /** Messages whose To, Cc or Bcc contains the address. */
  public List<Message> to(String address) {
    return all().stream()
        .filter(
            m ->
                m.to().stream().anyMatch(address::equalsIgnoreCase)
                    || m.cc().stream().anyMatch(address::equalsIgnoreCase)
                    || m.bcc().stream().anyMatch(address::equalsIgnoreCase))
        .toList();
  }

  /** Waits until at least {@code count} messages were received, at most ten seconds. */
  public List<Message> awaitAtLeast(int count) {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (total() < count && System.nanoTime() < deadline) {
      sleep(100);
    }
    return all();
  }

  /** Raw header values of a message, header name to values. */
  public Map<String, List<String>> headers(String id) {
    return JsonPath.read(get("/api/v1/message/" + id + "/headers"), "$");
  }

  public byte[] part(String id, String partId) {
    HttpResponse<byte[]> response =
        sendBytes(
            HttpRequest.newBuilder(
                    URI.create(
                        baseUrl
                            + "/api/v1/message/"
                            + id
                            + "/part/"
                            + URLEncoder.encode(partId, StandardCharsets.UTF_8)))
                .GET()
                .build());
    return response.body();
  }

  private Message message(String id) {
    String json = get("/api/v1/message/" + id);
    return new Message(
        id,
        JsonPath.read(json, "$.Subject"),
        JsonPath.read(json, "$.To[*].Address"),
        JsonPath.read(json, "$.Cc[*].Address"),
        JsonPath.read(json, "$.Bcc[*].Address"),
        JsonPath.read(json, "$.Text"),
        JsonPath.read(json, "$.HTML"),
        JsonPath.read(json, "$.Attachments"));
  }

  private String get(String path) {
    HttpResponse<byte[]> response =
        sendBytes(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build());
    return new String(response.body(), StandardCharsets.UTF_8);
  }

  private void send(HttpRequest request) {
    sendBytes(request);
  }

  private HttpResponse<byte[]> sendBytes(HttpRequest request) {
    try {
      HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 300) {
        throw new IllegalStateException(
            "Mailpit " + request.uri() + " answered " + response.statusCode());
      }
      return response;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
