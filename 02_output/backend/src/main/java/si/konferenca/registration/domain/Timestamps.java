package si.konferenca.registration.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** UTC instants with millisecond precision, as in the JSON copy and the export. */
public final class Timestamps {

  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private Timestamps() {}

  public static String format(Instant instant) {
    return FORMAT.format(instant);
  }
}
