package si.konferenca.registration.web;

import java.nio.file.Path;
import java.util.List;
import si.konferenca.registration.config.AppProperties;

/** Settings for unit tests of the web package. */
final class WebTestSupport {

  private WebTestSupport() {}

  static AppProperties properties(boolean httpsOnly, int perMinute, int maxBodyBytes) {
    return new AppProperties(
        "test",
        new AppProperties.Conference("classpath:conference.json", "Conf"),
        new AppProperties.JsonCopy(Path.of("copies")),
        new AppProperties.Recaptcha(true, null, "", "http://localhost"),
        new AppProperties.Organizer("o", "p", List.of("o@example.org"), httpsOnly),
        new AppProperties.Mail("from@example.org"),
        new AppProperties.Limits(perMinute, perMinute + 1, perMinute + 2, maxBodyBytes),
        new AppProperties.Cors(""));
  }
}
