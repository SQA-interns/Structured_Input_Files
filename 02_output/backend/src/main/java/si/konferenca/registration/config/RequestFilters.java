package si.konferenca.registration.config;

import jakarta.servlet.Filter;
import java.util.List;

/** Servlet filters placed in front of authentication, in this order. */
final class RequestFilters {

  private RequestFilters() {}

  static List<Filter> create(AppProperties properties) {
    return List.of();
  }
}
