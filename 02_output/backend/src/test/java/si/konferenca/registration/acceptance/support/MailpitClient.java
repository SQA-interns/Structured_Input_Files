package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the messages caught by Mailpit through its HTTP API. */
public final class MailpitClient {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final HttpClient http = HttpClient.newHttpClient();
  private final String baseUrl;

  MailpitClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public void deleteAll() {
    send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/messages")).DELETE().build());
  }

  /** Full message details (To, Cc, Bcc, Subject, Text, HTML, Attachments) of every message. */
  public List<JsonNode> allMessages() {
    JsonNode list = JSON.readTree(get("/api/v1/messages?limit=1000"));
    List<JsonNode> result = new ArrayList<>();
    for (JsonNode summary : list.path("messages")) {
      result.add(JSON.readTree(get("/api/v1/message/" + summary.path("ID").asString())));
    }
    return result;
  }

  /** Messages that have the address among their To recipients. */
  public List<JsonNode> messagesTo(String address) {
    List<JsonNode> result = new ArrayList<>();
    for (JsonNode message : allMessages()) {
      if (recipients(message, "To").contains(address.toLowerCase())) {
        result.add(message);
      }
    }
    return result;
  }

  /** Waits until at least {@code count} messages to the address arrived, at most 15 seconds. */
  public List<JsonNode> awaitMessagesTo(String address, int count) {
    long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    List<JsonNode> found = messagesTo(address);
    while (found.size() < count && System.nanoTime() < deadline) {
      pause(250);
      found = messagesTo(address);
    }
    return found;
  }

  /** Waits a fixed time so that a wrongly sent message would have arrived, then lists them. */
  public List<JsonNode> messagesToAfterGracePeriod(String address) {
    pause(1000);
    return messagesTo(address);
  }

  public static List<String> recipients(JsonNode message, String field) {
    List<String> result = new ArrayList<>();
    for (JsonNode r : message.path(field)) {
      result.add(r.path("Address").asString().toLowerCase());
    }
    return result;
  }

  /** Raw header map of a message: header name to list of values. */
  public JsonNode headers(String messageId) {
    return JSON.readTree(get("/api/v1/message/" + messageId + "/headers"));
  }

  public byte[] part(String messageId, String partId) {
    return sendBytes(
        HttpRequest.newBuilder(
                URI.create(baseUrl + "/api/v1/message/" + messageId + "/part/" + partId))
            .GET()
            .build());
  }

  private String get(String path) {
    return new String(
        sendBytes(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build()),
        StandardCharsets.UTF_8);
  }

  private void send(HttpRequest request) {
    sendBytes(request);
  }

  private byte[] sendBytes(HttpRequest request) {
    try {
      HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 300) {
        throw new IllegalStateException("Mailpit answered " + response.statusCode());
      }
      return response.body();
    } catch (IOException e) {
      throw new IllegalStateException("Mailpit not reachable", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static void pause(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
