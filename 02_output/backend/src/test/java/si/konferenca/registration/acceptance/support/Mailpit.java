package si.konferenca.registration.acceptance.support;

import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Client of the Mailpit HTTP API, used to observe the emails the backend sent. */
public final class Mailpit {

  private static final Configuration LENIENT =
      Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);

  private final HttpClient client =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  private final String baseUrl =
      "http://"
          + TestEnvironment.MAILPIT.getHost()
          + ":"
          + TestEnvironment.MAILPIT.getMappedPort(8025);

  /** Full messages (Mailpit message JSON) that have the address among their recipients. */
  public List<DocumentContext> messagesTo(String address) {
    DocumentContext list = JsonPath.using(LENIENT).parse(getText("/api/v1/messages?limit=1000"));
    List<Object> summaries = list.read("$.messages[*]");
    List<DocumentContext> result = new ArrayList<>();
    for (Object summary : summaries) {
      DocumentContext s = JsonPath.using(LENIENT).parse(summary);
      if (recipients(s).stream().anyMatch(a -> a.equalsIgnoreCase(address))) {
        result.add(message(s.read("$.ID")));
      }
    }
    return result;
  }

  /** Waits until a message to the address matching the condition arrives and returns it. */
  public DocumentContext awaitMessageTo(String address, Predicate<DocumentContext> condition) {
    List<DocumentContext> found = new ArrayList<>();
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(300))
        .until(
            () -> {
              found.clear();
              messagesTo(address).stream().filter(condition).forEach(found::add);
              return !found.isEmpty();
            });
    return found.get(0);
  }

  public DocumentContext awaitMessageTo(String address) {
    return awaitMessageTo(address, m -> true);
  }

  public DocumentContext message(String id) {
    return JsonPath.using(LENIENT).parse(getText("/api/v1/message/" + id));
  }

  /** All addresses in To, Cc and Bcc. */
  public static List<String> recipients(DocumentContext message) {
    List<String> all = new ArrayList<>();
    for (String field : List.of("To", "Cc", "Bcc")) {
      List<String> addresses = message.read("$." + field + "[*].Address");
      if (addresses != null) {
        all.addAll(addresses);
      }
    }
    return all;
  }

  public Map<String, List<String>> headers(String id) {
    return JsonPath.using(LENIENT).parse(getText("/api/v1/message/" + id + "/headers")).read("$");
  }

  public byte[] part(String id, String partId) {
    return get("/api/v1/message/" + id + "/part/" + partId);
  }

  private String getText(String path) {
    return new String(get(path), java.nio.charset.StandardCharsets.UTF_8);
  }

  private byte[] get(String path) {
    try {
      HttpResponse<byte[]> r =
          client.send(
              HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
              HttpResponse.BodyHandlers.ofByteArray());
      if (r.statusCode() != 200) {
        throw new IllegalStateException("Mailpit " + path + " answered " + r.statusCode());
      }
      return r.body();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
