package si.konferenca.registration.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.DefaultResourceLoader;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

class ConferenceCatalogParserTest {

  private static final String CONSENTS =
      "\"consents\":[{\"id\":\"privacy\",\"text\":\"I agree\",\"mandatory\":true}]";

  private static ConferenceCatalog parse(String json) {
    return new ConferenceCatalogParser()
        .parse(json.replace('\'', '"').getBytes(StandardCharsets.UTF_8));
  }

  private static String withOption(String option) {
    return "{'options':[" + option + "]," + CONSENTS.replace('"', '\'') + "}";
  }

  @Test
  void parsesOptionsWithDefaultsAndConsents() {
    ConferenceCatalog catalog =
        parse(
            "{'options':["
                + "{'id':'ws-1','name':'W','category':'WORKSHOP'},"
                + "{'id':'ev','name':'E','category':'EVENT','active':false,'availableTo':['STUDENT']}],"
                + "'consents':[{'id':'privacy','text':'I agree','mandatory':true},"
                + "{'id':'photos','text':'Photos','mandatory':false}]}");

    assertThat(catalog.options())
        .containsExactly(
            new ConferenceOption(
                "ws-1",
                "W",
                OptionCategory.WORKSHOP,
                true,
                java.util.EnumSet.allOf(RegistrationType.class)),
            new ConferenceOption(
                "ev",
                "E",
                OptionCategory.EVENT,
                false,
                java.util.EnumSet.of(RegistrationType.STUDENT)));
    assertThat(catalog.consents())
        .containsExactly(
            new ConsentDefinition("privacy", "I agree", true),
            new ConsentDefinition("photos", "Photos", false));
  }

  @Test
  void acceptsEveryCategoryAndBothTypes() {
    ConferenceCatalog catalog =
        parse(
            "{'options':[{'id':'a','name':'A','category':'MEAL','availableTo':['EXTERNAL','STUDENT']},"
                + "{'id':'b','name':'B','category':'OTHER','active':true}],'consents':[]}");

    assertThat(catalog.options())
        .extracting(ConferenceOption::category)
        .containsExactly(OptionCategory.MEAL, OptionCategory.OTHER);
    assertThat(catalog.options().get(0).availableTo())
        .containsExactlyInAnyOrder(RegistrationType.EXTERNAL, RegistrationType.STUDENT);
    assertThat(catalog.options().get(1).active()).isTrue();
  }

  @Test
  void nameAndTextMaximumLengthsAreAccepted() {
    String name = "n".repeat(200);
    String text = "t".repeat(1000);
    ConferenceCatalog catalog =
        parse(
            "{'options':[{'id':'a','name':'"
                + name
                + "','category':'MEAL'}],"
                + "'consents':[{'id':'c','text':'"
                + text
                + "','mandatory':false}]}");

    assertThat(catalog.options().get(0).name()).hasSize(200);
    assertThat(catalog.consents().get(0).text()).hasSize(1000);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      value = {
        "not JSON|{options:|not valid JSON",
        "not an object|[]|must be a JSON object",
        "unknown root field|{'options':[],'consents':[],'x':1}|unknown field x in configuration",
        "options missing|{'consents':[]}|options must be an array",
        "consents not array|{'options':[],'consents':{}}|consents must be an array",
        "option not object|{'options':[1],'consents':[]}|every option must be an object",
        "unknown option field|{'options':[{'id':'a','name':'A','category':'MEAL','x':1}],'consents':[]}|unknown field x in option",
        "missing id|{'options':[{'name':'A','category':'MEAL'}],'consents':[]}|option id missing",
        "numeric id|{'options':[{'id':1,'name':'A','category':'MEAL'}],'consents':[]}|option id missing",
        "bad id|{'options':[{'id':'A b','name':'A','category':'MEAL'}],'consents':[]}|option id missing",
        "duplicate id|{'options':[{'id':'a','name':'A','category':'MEAL'},{'id':'a','name':'B','category':'MEAL'}],'consents':[]}|duplicate option id a",
        "missing name|{'options':[{'id':'a','category':'MEAL'}],'consents':[]}|option a needs a non-blank name",
        "blank name|{'options':[{'id':'a','name':'  ','category':'MEAL'}],'consents':[]}|option a needs a non-blank name",
        "numeric name|{'options':[{'id':'a','name':5,'category':'MEAL'}],'consents':[]}|option a needs a non-blank name",
        "missing category|{'options':[{'id':'a','name':'A'}],'consents':[]}|option a needs a category",
        "numeric category|{'options':[{'id':'a','name':'A','category':1}],'consents':[]}|option a needs a category",
        "unknown category|{'options':[{'id':'a','name':'A','category':'SPA'}],'consents':[]}|unknown category SPA",
        "active not boolean|{'options':[{'id':'a','name':'A','category':'MEAL','active':'yes'}],'consents':[]}|active must be true or false",
        "availableTo empty|{'options':[{'id':'a','name':'A','category':'MEAL','availableTo':[]}],'consents':[]}|availableTo must be a non-empty array",
        "availableTo not array|{'options':[{'id':'a','name':'A','category':'MEAL','availableTo':'STUDENT'}],'consents':[]}|availableTo must be a non-empty array",
        "unknown type|{'options':[{'id':'a','name':'A','category':'MEAL','availableTo':['GUEST']}],'consents':[]}|unknown registration type",
        "numeric type|{'options':[{'id':'a','name':'A','category':'MEAL','availableTo':[1]}],'consents':[]}|unknown registration type",
        "type twice|{'options':[{'id':'a','name':'A','category':'MEAL','availableTo':['STUDENT','STUDENT']}],'consents':[]}|lists STUDENT twice",
        "consent not object|{'options':[],'consents':['x']}|every consent must be an object",
        "unknown consent field|{'options':[],'consents':[{'id':'c','text':'T','mandatory':true,'x':1}]}|unknown field x in consent",
        "consent without text|{'options':[],'consents':[{'id':'c','mandatory':true}]}|consent c needs a non-blank text",
        "consent mandatory missing|{'options':[],'consents':[{'id':'c','text':'T'}]}|consent c needs a boolean mandatory flag",
        "consent mandatory text|{'options':[],'consents':[{'id':'c','text':'T','mandatory':'yes'}]}|consent c needs a boolean mandatory flag",
        "duplicate consent|{'options':[],'consents':[{'id':'c','text':'T','mandatory':true},{'id':'c','text':'U','mandatory':false}]}|duplicate consent id c",
        "bad consent id|{'options':[],'consents':[{'id':'-c','text':'T','mandatory':true}]}|consent id missing"
      })
  void invalidConfigurationsAreRejectedWithTheReason(String reason, String json, String message) {
    assertThatThrownBy(() -> parse(json))
        .isInstanceOf(InvalidConferenceConfigurationException.class)
        .hasMessageContaining(message);
  }

  @Test
  void overlongNameAndTextAreRejected() {
    assertThatThrownBy(
            () ->
                parse(withOption("{'id':'a','name':'" + "n".repeat(201) + "','category':'MEAL'}")))
        .hasMessageContaining("option a name is longer than 200");
    assertThatThrownBy(
            () ->
                parse(
                    "{'options':[],'consents':[{'id':'c','text':'"
                        + "t".repeat(1001)
                        + "','mandatory':true}]}"))
        .hasMessageContaining("consent c text is longer than 1000");
  }

  @Test
  void idPatternAllowsSixtyFourCharacters() {
    String id64 = "a" + "-".repeat(63);
    assertThat(parse(withOption("{'id':'" + id64 + "','name':'A','category':'MEAL'}")).options())
        .hasSize(1);
    assertThatThrownBy(
            () -> parse(withOption("{'id':'" + id64 + "x','name':'A','category':'MEAL'}")))
        .hasMessageContaining("id missing");
  }

  @Test
  void loaderReadsAResourceAndRejectsAMissingOne() {
    CatalogConfiguration configuration = new CatalogConfiguration();
    DefaultResourceLoader loader = new DefaultResourceLoader();

    assertThat(
            configuration
                .conferenceCatalog(loader, "classpath:acceptance/conference-acceptance.json")
                .options())
        .isNotEmpty();
    assertThatThrownBy(
            () -> configuration.conferenceCatalog(loader, "classpath:does-not-exist.json"))
        .isInstanceOf(InvalidConferenceConfigurationException.class)
        .hasMessage("Conference configuration not found: classpath:does-not-exist.json");
    assertThat(
            new InvalidConferenceConfigurationException("m", new IllegalStateException())
                .getCause())
        .isInstanceOf(IllegalStateException.class);
  }
}
