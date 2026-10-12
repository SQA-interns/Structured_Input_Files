package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the messages Mailpit received, through its HTTP API. */
public final class MailpitClient {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final HttpClient http = HttpClient.newHttpClient();
  private final String baseUrl;

  public MailpitClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** Summaries of all messages with {@code address} among the recipients. */
  public List<JsonNode> messagesTo(String address) {
    JsonNode result = getJson("/api/v1/search?limit=500&query=" + encode("to:\"" + address + "\""));
    List<JsonNode> list = new ArrayList<>();
    result.path("messages").forEach(list::add);
    return list;
  }

  /** Waits up to five seconds until {@code condition} holds for the messages to {@code address}. */
  public List<JsonNode> awaitMessagesTo(String address, Predicate<List<JsonNode>> condition) {
    long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
    List<JsonNode> messages = messagesTo(address);
    while (!condition.test(messages) && System.nanoTime() < deadline) {
      sleep(200);
      messages = messagesTo(address);
    }
    return messages;
  }

  /** Full message (From, To, Cc, Bcc, Subject, Text, HTML, Attachments). */
  public JsonNode message(String id) {
    return getJson("/api/v1/message/" + id);
  }

  /** Header names and values of a message. */
  public JsonNode headers(String id) {
    return getJson("/api/v1/message/" + id + "/headers");
  }

  /** Raw bytes of one MIME part (for example an attachment). */
  public byte[] part(String id, String partId) {
    return get("/api/v1/message/" + id + "/part/" + partId, HttpResponse.BodyHandlers.ofByteArray())
        .body();
  }

  /** Number of messages whose content mentions {@code text}. */
  public long countMentioning(String text) {
    sleep(300);
    return getJson("/api/v1/search?query=" + encode("\"" + text + "\""))
        .path("messages_count")
        .asLong();
  }

  private JsonNode getJson(String path) {
    return JSON.readTree(get(path, HttpResponse.BodyHandlers.ofString()).body());
  }

  private <T> HttpResponse<T> get(String path, HttpResponse.BodyHandler<T> handler) {
    try {
      HttpResponse<T> response =
          http.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), handler);
      if (response.statusCode() != 200) {
        throw new IllegalStateException("Mailpit " + path + " answered " + response.statusCode());
      }
      return response;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
