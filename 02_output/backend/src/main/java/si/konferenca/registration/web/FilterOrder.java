package si.konferenca.registration.web;

/** Order of the request filters; all run before the Spring Security chain (order -100). */
final class FilterOrder {

  static final int RATE_LIMIT = -300;
  static final int BODY_SIZE = -250;
  static final int HTTPS_ONLY = -200;

  private FilterOrder() {}
}
