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

/** Reads delivered emails through the Mailpit HTTP API (the local SMTP substitute). */
public final class Mailpit {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final HttpClient http = HttpClient.newHttpClient();
  private final String baseUrl;

  public Mailpit(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** Summary entries of all messages whose To list contains the address. */
  public List<JsonNode> messagesTo(String address) {
    JsonNode result = getJson("/api/v1/search?limit=200&query=" + encode("to:\"" + address + "\""));
    List<JsonNode> matches = new ArrayList<>();
    for (JsonNode message : result.path("messages")) {
      for (JsonNode to : message.path("To")) {
        if (address.equalsIgnoreCase(to.path("Address").asString())) {
          matches.add(message);
          break;
        }
      }
    }
    return matches;
  }

  /** Waits until at least {@code count} messages to the address match the filter. */
  public List<JsonNode> awaitMessagesTo(
      String address, Predicate<JsonNode> filter, int count, Duration timeout) {
    long deadline = System.nanoTime() + timeout.toNanos();
    List<JsonNode> found = List.of();
    while (System.nanoTime() < deadline) {
      found = messagesTo(address).stream().filter(filter).toList();
      if (found.size() >= count) {
        return found;
      }
      sleep(250);
    }
    throw new AssertionError(
        "expected " + count + " message(s) to " + address + " but found " + found.size());
  }

  public JsonNode awaitSingleMessageTo(String address, Predicate<JsonNode> filter) {
    List<JsonNode> found = awaitMessagesTo(address, filter, 1, Duration.ofSeconds(20));
    return message(found.get(0).path("ID").asString());
  }

  /** Asserts that no message to the address arrives within the wait time. */
  public void assertNoMessageTo(String address, Duration wait) {
    sleep(wait.toMillis());
    List<JsonNode> found = messagesTo(address);
    if (!found.isEmpty()) {
      throw new AssertionError("expected no message to " + address + " but found " + found.size());
    }
  }

  /** Full message: From, To, Subject, Text, HTML, Attachments[PartID, FileName, ContentType]. */
  public JsonNode message(String id) {
    return getJson("/api/v1/message/" + id);
  }

  public JsonNode headers(String id) {
    return getJson("/api/v1/message/" + id + "/headers");
  }

  public byte[] part(String id, String partId) {
    return get(
        "/api/v1/message/" + id + "/part/" + partId, HttpResponse.BodyHandlers.ofByteArray());
  }

  public String raw(String id) {
    return new String(
        get("/api/v1/message/" + id + "/raw", HttpResponse.BodyHandlers.ofByteArray()),
        StandardCharsets.UTF_8);
  }

  private JsonNode getJson(String path) {
    return JSON.readTree(
        new String(get(path, HttpResponse.BodyHandlers.ofByteArray()), StandardCharsets.UTF_8));
  }

  private <T> T get(String path, HttpResponse.BodyHandler<T> handler) {
    try {
      HttpResponse<T> response =
          http.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), handler);
      if (response.statusCode() != 200) {
        throw new IllegalStateException("Mailpit " + path + " -> " + response.statusCode());
      }
      return response.body();
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
