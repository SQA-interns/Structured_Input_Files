package si.konferenca.registration.adapter.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

class JsonFileConferenceCatalogTest {

  private static final String CONSENTS =
      "\"consents\": [{\"id\": \"privacy\", \"text\": \"Text\", \"mandatory\": true}]";

  private static JsonFileConferenceCatalog parse(String json) {
    return JsonFileConferenceCatalog.parse(json.getBytes(StandardCharsets.UTF_8));
  }

  private static String withOption(String option) {
    return "{\"options\": [" + option + "], " + CONSENTS + "}";
  }

  @Test
  void parsesOptionsAndConsents() {
    JsonFileConferenceCatalog c =
        parse(
            withOption(
                "{\"id\": \"ws-1\", \"name\": \"Č workshop\", \"category\": \"WORKSHOP\","
                    + " \"active\": false, \"availableTo\": [\"STUDENT\"]}"));

    assertThat(c.options()).hasSize(1);
    assertThat(c.options().get(0).name()).isEqualTo("Č workshop");
    assertThat(c.options().get(0).category()).isEqualTo(OptionCategory.WORKSHOP);
    assertThat(c.options().get(0).active()).isFalse();
    assertThat(c.options().get(0).availableTo()).containsExactly(RegistrationType.STUDENT);
    assertThat(c.consents().get(0).mandatory()).isTrue();
    assertThat(c.consents().get(0).text()).isEqualTo("Text");
  }

  @Test
  void availableToDefaultsToBothTypes() {
    JsonFileConferenceCatalog c =
        parse(
            withOption(
                "{\"id\": \"m\", \"name\": \"Meal\", \"category\": \"MEAL\", \"active\": true}"));

    assertThat(c.options().get(0).availableTo())
        .containsExactlyInAnyOrder(RegistrationType.EXTERNAL, RegistrationType.STUDENT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not json",
        "[]",
        "{\"options\": {}, \"consents\": []}",
        "{\"options\": [], \"consents\": [], \"extra\": 1}",
        "{\"options\": [1], \"consents\": []}",
        "{\"options\": [{\"id\": \"Bad Id\", \"name\": \"n\", \"category\": \"MEAL\", \"active\": true}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"\", \"category\": \"MEAL\", \"active\": true}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"n\", \"category\": \"TRIP\", \"active\": true}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"n\", \"category\": \"MEAL\", \"active\": \"yes\"}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"n\", \"category\": \"MEAL\", \"active\": true, \"x\": 1}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"n\", \"category\": \"MEAL\", \"active\": true, \"availableTo\": []}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"n\", \"category\": \"MEAL\", \"active\": true, \"availableTo\": [\"STUDENT\", \"STUDENT\"]}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"n\", \"category\": \"MEAL\", \"active\": true, \"availableTo\": [\"GUEST\"]}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"n\", \"category\": \"MEAL\", \"active\": true}, {\"id\": \"a\", \"name\": \"m\", \"category\": \"MEAL\", \"active\": true}], \"consents\": []}",
        "{\"options\": [], \"consents\": [{\"id\": \"p\", \"text\": \"t\", \"mandatory\": true}, {\"id\": \"p\", \"text\": \"u\", \"mandatory\": false}]}",
        "{\"options\": [], \"consents\": [{\"id\": \"p\", \"text\": \" \", \"mandatory\": true}]}"
      })
  void rejectsInvalidConfigurations(String json) {
    assertThatThrownBy(() -> parse(json))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("Invalid conference configuration");
  }

  @Test
  void rejectsTooLongNames() {
    String name = "n".repeat(201);
    assertThatThrownBy(
            () ->
                parse(
                    withOption(
                        "{\"id\": \"a\", \"name\": \""
                            + name
                            + "\", \"category\": \"MEAL\", \"active\": true}")))
        .isInstanceOf(IllegalStateException.class);
    assertThat(
            parse(
                    withOption(
                        "{\"id\": \"a\", \"name\": \""
                            + "n".repeat(200)
                            + "\", \"category\": \"MEAL\", \"active\": true}"))
                .options())
        .hasSize(1);
  }

  @Test
  void loadsAFileAndTheBundledDefault(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("c.json");
    Files.writeString(
        file,
        withOption("{\"id\": \"a\", \"name\": \"n\", \"category\": \"OTHER\", \"active\": true}"));

    assertThat(JsonFileConferenceCatalog.load(file).options()).hasSize(1);
    assertThat(JsonFileConferenceCatalog.load(null).options()).isNotEmpty();
    assertThat(JsonFileConferenceCatalog.load(null).consents())
        .anySatisfy(c -> assertThat(c.mandatory()).isTrue());
  }

  @Test
  void missingFileStopsTheStart(@TempDir Path dir) {
    assertThatThrownBy(() -> JsonFileConferenceCatalog.load(dir.resolve("missing.json")))
        .isInstanceOf(IllegalStateException.class);
  }
}
