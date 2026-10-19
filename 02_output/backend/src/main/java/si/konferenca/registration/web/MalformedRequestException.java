package si.konferenca.registration.web;

/** The request body is not a RegistrationRequest (unknown property or wrong JSON type). */
class MalformedRequestException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  MalformedRequestException() {
    super("Malformed request");
  }
}
