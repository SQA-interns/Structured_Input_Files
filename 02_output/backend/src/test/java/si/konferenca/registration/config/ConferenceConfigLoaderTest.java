package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.DefaultResourceLoader;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** AR-04, AC-003-03: every schema violation stops start-up with a message naming it. */
class ConferenceConfigLoaderTest {

  private static final String CONSENTS =
      "\"consents\":[{\"id\":\"p\",\"text\":\"T\",\"mandatory\":true}]";

  private static ConferenceCatalogue load(String json) {
    return ConferenceConfigLoader.load(
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), "test.json");
  }

  private static String options(String... options) {
    return "{\"options\":[" + String.join(",", options) + "]," + CONSENTS + "}";
  }

  static Stream<Arguments> invalid() {
    String longName = "n".repeat(201);
    String longText = "t".repeat(1001);
    return Stream.of(
        Arguments.of("{", "test.json is not valid JSON"),
        Arguments.of("", "must be a JSON object"),
        Arguments.of("[]", "must be a JSON object"),
        Arguments.of("{\"options\":[]," + CONSENTS + ",\"x\":1}", "unknown property 'x'"),
        Arguments.of("{\"options\":{}," + CONSENTS + "}", "needs an array 'options'"),
        Arguments.of("{\"options\":[]}", "needs an array 'consents'"),
        Arguments.of(options("1"), "Every option must be a JSON object"),
        Arguments.of(options("{\"name\":\"N\",\"category\":\"MEAL\"}"), "Option without id"),
        Arguments.of(
            options("{\"id\":5,\"name\":\"N\",\"category\":\"MEAL\"}"), "Option without id"),
        Arguments.of(
            options("{\"id\":\"Bad_ID\",\"name\":\"N\",\"category\":\"MEAL\"}"),
            "Option id 'Bad_ID' is not a valid id"),
        Arguments.of(
            options("{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\",\"price\":1}"),
            "Option 'a' has unknown property 'price'"),
        Arguments.of(
            options("{\"id\":\"a\",\"name\":\"  \",\"category\":\"MEAL\"}"),
            "Option 'a' has no name"),
        Arguments.of(
            options("{\"id\":\"a\",\"name\":\"" + longName + "\",\"category\":\"MEAL\"}"),
            "Option 'a' has no name shorter than 201"),
        Arguments.of(options("{\"id\":\"a\",\"name\":\"N\"}"), "Option 'a' has no category"),
        Arguments.of(
            options("{\"id\":\"a\",\"name\":\"N\",\"category\":3}"), "Option 'a' has no category"),
        Arguments.of(
            options("{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\",\"active\":\"yes\"}"),
            "Option 'a' has a non-boolean 'active'"),
        Arguments.of(
            options(
                "{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\",\"availableTo\":\"EXTERNAL\"}"),
            "must list at least one registration type"),
        Arguments.of(
            options("{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\",\"availableTo\":[]}"),
            "must list at least one registration type"),
        Arguments.of(
            options(
                "{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\",\"availableTo\":[\"VIP\"]}"),
            "Option 'a' has unknown registration type 'VIP'"),
        Arguments.of(
            options("{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\",\"availableTo\":[1]}"),
            "Option 'a' has a non-text registration type"),
        Arguments.of(
            options(
                "{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\",\"availableTo\":[\"STUDENT\",\"STUDENT\"]}"),
            "lists registration type 'STUDENT' twice"),
        Arguments.of(
            "{\"options\":[],\"consents\":[\"x\"]}", "Every consent must be a JSON object"),
        Arguments.of(
            "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\"T\"}]}",
            "Consent 'p' needs a boolean 'mandatory'"),
        Arguments.of(
            "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\"T\",\"mandatory\":\"yes\"}]}",
            "Consent 'p' needs a boolean 'mandatory'"),
        Arguments.of(
            "{\"options\":[],\"consents\":[{\"id\":\"p\",\"mandatory\":true}]}",
            "Consent 'p' has no text"),
        Arguments.of(
            "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\""
                + longText
                + "\",\"mandatory\":true}]}",
            "Consent 'p' has no text shorter than 1001"),
        Arguments.of(
            "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\"T\",\"mandatory\":true,\"x\":1}]}",
            "Consent 'p' has unknown property 'x'"),
        Arguments.of(
            "{\"options\":[],\"consents\":[{\"id\":\"p\",\"text\":\"T\",\"mandatory\":true},"
                + "{\"id\":\"p\",\"text\":\"U\",\"mandatory\":false}]}",
            "Duplicate consent id 'p'"),
        Arguments.of(
            options(
                "{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\"}",
                "{\"id\":\"a\",\"name\":\"M\",\"category\":\"MEAL\"}"),
            "Duplicate option id 'a'"));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("invalid")
  void invalidConfigurationIsRejectedWithItsProblem(String json, String problem) {
    assertThatThrownBy(() -> load(json))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining(problem);
  }

  @Test
  void defaultsAreActiveAndAvailableToBothTypes() {
    ConferenceCatalogue catalogue =
        load(
            options(
                "{\"id\":\"a\",\"name\":\"N\",\"category\":\"MEAL\"}",
                "{\"id\":\"b-2\",\"name\":\"M\",\"category\":\"OTHER\",\"active\":false,"
                    + "\"availableTo\":[\"STUDENT\"]}"));

    assertThat(catalogue.options())
        .containsExactly(
            new ConferenceOption(
                "a", "N", OptionCategory.MEAL, true, EnumSet.allOf(RegistrationType.class)),
            new ConferenceOption(
                "b-2", "M", OptionCategory.OTHER, false, EnumSet.of(RegistrationType.STUDENT)));
    assertThat(catalogue.activeOptions()).extracting(ConferenceOption::id).containsExactly("a");
    assertThat(catalogue.consents()).hasSize(1);
    assertThat(catalogue.consent("p")).isPresent();
    assertThat(catalogue.consent("x")).isEmpty();
  }

  @Test
  void inputIsClosedAfterReading() {
    boolean[] closed = {false};
    InputStream in =
        new ByteArrayInputStream(options().getBytes(StandardCharsets.UTF_8)) {
          @Override
          public void close() throws IOException {
            closed[0] = true;
            super.close();
          }
        };
    ConferenceConfigLoader.load(in, "x");
    assertThat(closed[0]).isTrue();
  }

  @Test
  void missingConfigurationFileStopsStartUp() {
    ApplicationConfiguration config = new ApplicationConfiguration();
    assertThatThrownBy(
            () ->
                config.conferenceCatalogue(
                    new DefaultResourceLoader(), "classpath:does-not-exist.json"))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void shippedConfigurationLoads() {
    ApplicationConfiguration config = new ApplicationConfiguration();
    ConferenceCatalogue catalogue =
        config.conferenceCatalogue(new DefaultResourceLoader(), "file:config/conference.json");
    assertThat(catalogue.activeOptions()).isNotEmpty();
    assertThat(catalogue.consent("privacy"))
        .hasValueSatisfying(c -> assertThat(c.mandatory()).isTrue());
    assertThat(config.clock()).isNotNull();
    assertThat(config.formSettings("C", true, "k").captchaTestMode()).isTrue();
  }
}
