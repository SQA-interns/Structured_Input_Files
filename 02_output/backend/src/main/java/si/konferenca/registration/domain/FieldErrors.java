package si.konferenca.registration.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Field key to message; the first message per field wins (spec 4.2). */
public final class FieldErrors {

  private final Map<String, String> errors = new LinkedHashMap<>();

  public void add(String field, String message) {
    errors.putIfAbsent(field, message);
  }

  public void addAll(FieldErrors other) {
    other.errors.forEach(this::add);
  }

  public boolean isEmpty() {
    return errors.isEmpty();
  }

  public Map<String, String> asMap() {
    return Collections.unmodifiableMap(errors);
  }
}
