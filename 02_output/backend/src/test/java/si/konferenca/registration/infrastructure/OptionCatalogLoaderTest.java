package si.konferenca.registration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

class OptionCatalogLoaderTest {

  private static final String CONSENTS =
      "\"consents\": [{\"id\": \"data\", \"text\": \" Soglasje \", \"mandatory\": true}]";

  private static OptionCatalog load(String json) {
    return new OptionCatalogLoader()
        .load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), "test.json");
  }

  private static String options(String option) {
    return "{\"options\": [" + option + "], " + CONSENTS + "}";
  }

  @Test
  void loadsOptionsAndConsents() {
    OptionCatalog catalog =
        load(
            options(
                "{\"id\": \"ws-a\", \"name\": \" Delavnica \", \"category\": \"WORKSHOP\","
                    + " \"active\": false, \"audience\": [\"STUDENT\"]}"));

    assertThat(catalog.options()).hasSize(1);
    var option = catalog.options().get(0);
    assertThat(option.name()).isEqualTo("Delavnica");
    assertThat(option.category()).isEqualTo(OptionCategory.WORKSHOP);
    assertThat(option.active()).isFalse();
    assertThat(option.audience()).containsExactly(RegistrationType.STUDENT);
    assertThat(catalog.consents().get(0).text()).isEqualTo("Soglasje");
    assertThat(catalog.consents().get(0).mandatory()).isTrue();
  }

  @Test
  void audienceDefaultsToBoth() {
    OptionCatalog catalog =
        load(options("{\"id\": \"m\", \"name\": \"M\", \"category\": \"MEAL\", \"active\": true}"));

    assertThat(catalog.options().get(0).audience())
        .containsExactlyInAnyOrder(RegistrationType.values());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '`',
      value = {
        "not json|not valid JSON",
        "[]|top level must be an object",
        "{\"options\": [], \"consents\": [], \"x\": 1}|unknown field 'x'",
        "{\"consents\": []}|'options' must be a list",
        "{\"options\": [], \"consents\": {}}|'consents' must be a list",
        "{\"options\": [1], \"consents\": []}|option #0 must be an object",
        "{\"options\": [{\"id\": \"Bad Id\"}], \"consents\": []}|option #0 has a missing or invalid 'id'",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\"}], \"consents\": []}|option 'a': 'active' must be true or false",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\", \"active\": true, \"audience\": []}], \"consents\": []}|option 'a': 'audience' must be a non-empty list",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\", \"active\": true, \"audience\": [1]}], \"consents\": []}|unknown audience '1'",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": 5, \"active\": true}], \"consents\": []}|unknown category '5'",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"meal\", \"active\": true}], \"consents\": []}|unknown category 'meal'",
        "{\"options\": [{\"id\": \"a\", \"name\": \"  \", \"category\": \"MEAL\", \"active\": true}], \"consents\": []}|'name' is missing or empty",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\", \"active\": true, \"price\": 1}], \"consents\": []}|option 'a': unknown field 'price'",
        "{\"options\": [], \"consents\": [{\"id\": \"c\", \"text\": \"T\"}]}|consent 'c': 'mandatory' must be true or false",
        "{\"options\": [], \"consents\": [{\"id\": \"c\", \"text\": \"T\", \"mandatory\": true}, {\"id\": \"c\", \"text\": \"T\", \"mandatory\": true}]}|consent 'c' is defined more than once",
        "{\"options\": [], \"consents\": [{\"id\": \"c\", \"text\": \"\", \"mandatory\": true}]}|consent 'c': 'text' is missing or empty"
      })
  void rejectsInvalidFilesNamingTheProblem(String json, String message) {
    assertThatThrownBy(() -> load(json))
        .isInstanceOf(InvalidOptionsException.class)
        .hasMessageContaining("test.json")
        .hasMessageContaining(message);
  }

  @Test
  void rejectsTooLongName() {
    String json =
        options(
            "{\"id\": \"a\", \"name\": \""
                + "n".repeat(201)
                + "\", \"category\": \"MEAL\", \"active\": true}");

    assertThatThrownBy(() -> load(json)).hasMessageContaining("longer than 200");
  }
}
