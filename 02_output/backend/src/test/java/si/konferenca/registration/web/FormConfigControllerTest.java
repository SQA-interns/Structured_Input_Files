package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.conference.Category;
import si.konferenca.registration.conference.ConferenceConfiguration;
import si.konferenca.registration.conference.ConferenceOption;
import si.konferenca.registration.conference.ConsentDefinition;
import si.konferenca.registration.domain.RegistrationType;

class FormConfigControllerTest {

  @Test
  void listsActiveOptionsWithSortedTypesAndAnEmptySiteKeyWhenUnset() {
    ConferenceConfiguration conference =
        new ConferenceConfiguration(
            List.of(
                new ConferenceOption(
                    "a",
                    "A",
                    Category.MEAL,
                    true,
                    EnumSet.of(RegistrationType.STUDENT, RegistrationType.EXTERNAL)),
                new ConferenceOption(
                    "b", "B", Category.EVENT, false, EnumSet.of(RegistrationType.STUDENT))),
            List.of(new ConsentDefinition("privacy", "P", true)));

    FormConfigController.FormConfig config =
        new FormConfigController(conference, WebTestSupport.properties(true, 1, 1)).formConfig();

    assertThat(config.conferenceName()).isEqualTo("Conf");
    assertThat(config.recaptcha()).isEqualTo(new FormConfigController.Recaptcha("", true));
    assertThat(config.options())
        .containsExactly(
            new FormConfigController.Option("a", "A", "MEAL", List.of("EXTERNAL", "STUDENT")));
    assertThat(config.consents()).containsExactly(new ConsentDefinition("privacy", "P", true));
  }
}
