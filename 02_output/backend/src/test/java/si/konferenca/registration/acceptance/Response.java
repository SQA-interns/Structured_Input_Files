package si.konferenca.registration.acceptance;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** An HTTP response with helpers to read JSON bodies and problem field errors. */
record Response(int status, byte[] bytes, Map<String, List<String>> headers) {

  String body() {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  String header(String name) {
    return headers.entrySet().stream()
        .filter(e -> e.getKey().equalsIgnoreCase(name))
        .flatMap(e -> e.getValue().stream())
        .findFirst()
        .orElse(null);
  }

  <T> T json(String path) {
    return JsonPath.read(body(), path);
  }

  /** Field names of the {@code fieldErrors} of a problem response; empty if there are none. */
  List<String> errorFields() {
    try {
      return JsonPath.read(body(), "$.fieldErrors[*].field");
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  @Override
  public String toString() {
    return "HTTP " + status + " " + body();
  }
}
