package si.konferenca.registration.acceptance;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Emails delivered to the Mailpit catcher, read through its REST API (v1). */
final class Mailbox {

  /** One delivered email. */
  record Message(
      String id,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String subject,
      String text,
      String html,
      List<Attachment> attachments,
      Map<String, List<String>> headers) {}

  /** One attachment of a delivered email. */
  record Attachment(String partId, String fileName, String contentType, byte[] content) {}

  private final HttpClient client = HttpClient.newHttpClient();
  private final String baseUrl;

  Mailbox(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  void deleteAll() throws Exception {
    client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/messages")).DELETE().build(),
        HttpResponse.BodyHandlers.discarding());
  }

  /** All messages, after a short pause so a message in flight is not missed. */
  List<Message> messagesAfterSettling() throws Exception {
    Thread.sleep(500);
    return messages();
  }

  /** Waits up to ten seconds until at least {@code count} messages arrived. */
  List<Message> awaitMessages(int count) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    List<Message> found = messages();
    while (found.size() < count && System.nanoTime() < deadline) {
      Thread.sleep(200);
      found = messages();
    }
    return found;
  }

  List<Message> messagesTo(List<Message> messages, String address) {
    return messages.stream().filter(m -> m.to().contains(address)).toList();
  }

  List<Message> messages() throws Exception {
    String list = getString("/api/v1/messages?limit=500");
    List<String> ids = JsonPath.read(list, "$.messages[*].ID");
    List<Message> result = new ArrayList<>();
    for (String id : ids) {
      result.add(message(id));
    }
    return result;
  }

  private Message message(String id) throws Exception {
    String json = getString("/api/v1/message/" + id);
    List<Map<String, Object>> parts = JsonPath.read(json, "$.Attachments");
    List<Attachment> attachments = new ArrayList<>();
    for (Map<String, Object> part : parts) {
      String partId = String.valueOf(part.get("PartID"));
      attachments.add(
          new Attachment(
              partId,
              String.valueOf(part.get("FileName")),
              String.valueOf(part.get("ContentType")),
              getBytes("/api/v1/message/" + id + "/part/" + partId)));
    }
    Map<String, List<String>> headers =
        JsonPath.read(getString("/api/v1/message/" + id + "/headers"), "$");
    return new Message(
        id,
        JsonPath.read(json, "$.To[*].Address"),
        JsonPath.read(json, "$.Cc[*].Address"),
        JsonPath.read(json, "$.Bcc[*].Address"),
        JsonPath.read(json, "$.Subject"),
        JsonPath.read(json, "$.Text"),
        JsonPath.read(json, "$.HTML"),
        attachments,
        headers);
  }

  private String getString(String path) throws Exception {
    return new String(getBytes(path), StandardCharsets.UTF_8);
  }

  private byte[] getBytes(String path) throws Exception {
    return client
        .send(
            HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray())
        .body();
  }
}
