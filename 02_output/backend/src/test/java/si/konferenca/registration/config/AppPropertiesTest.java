package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppPropertiesTest {

  @Test
  void copiesListsAndReplacesNullWithEmpty() {
    List<String> origins = new ArrayList<>(List.of("http://localhost:5173"));
    List<String> emails = new ArrayList<>(List.of("a@b.si"));
    AppProperties p =
        new AppProperties(
            "C",
            "o",
            "/d",
            null,
            new AppProperties.Organizer("u", "p", emails, true),
            null,
            origins,
            null,
            1);
    origins.clear();
    emails.clear();

    assertThat(p.corsAllowedOrigins()).containsExactly("http://localhost:5173");
    assertThat(p.organizer().emails()).containsExactly("a@b.si");
    assertThat(
            new AppProperties("C", "o", "/d", null, null, null, null, null, 1).corsAllowedOrigins())
        .isEmpty();
    assertThat(new AppProperties.Organizer("u", "p", null, true).emails()).isEmpty();
  }
}
