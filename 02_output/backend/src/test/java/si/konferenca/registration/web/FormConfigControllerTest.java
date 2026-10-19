package si.konferenca.registration.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import si.konferenca.registration.application.CaptchaSettings;
import si.konferenca.registration.application.FormConfigurationService;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** Covers AC-003-01. */
class FormConfigControllerTest {

  private final ConferenceConfiguration configuration =
      ConferenceConfiguration.of(
          "Konferenca",
          List.of(new ConsentDefinition("privacy", "I agree.", true)),
          List.of(
              new ConferenceOption("ws-a", "Workshop A", OptionCategory.WORKSHOP, true, null),
              new ConferenceOption("ws-old", "Old", OptionCategory.WORKSHOP, false, null),
              new ConferenceOption(
                  "oth-s",
                  "Mentoring",
                  OptionCategory.OTHER,
                  true,
                  Set.of(RegistrationType.STUDENT))));

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(
              new FormConfigController(
                  new FormConfigurationService(
                      configuration, new CaptchaSettings("site-key", false))))
          .build();

  @Test
  void AC_003_01_formConfigListsActiveOptionsConsentsAndCaptchaSettings() throws Exception {
    mvc.perform(get("/api/form-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conferenceName").value("Konferenca"))
        .andExpect(jsonPath("$.options.length()").value(2))
        .andExpect(jsonPath("$.options[0].id").value("ws-a"))
        .andExpect(jsonPath("$.options[0].name").value("Workshop A"))
        .andExpect(jsonPath("$.options[0].category").value("WORKSHOP"))
        .andExpect(jsonPath("$.options[0].availableTo.length()").value(2))
        .andExpect(jsonPath("$.options[1].id").value("oth-s"))
        .andExpect(jsonPath("$.options[1].availableTo[0]").value("STUDENT"))
        .andExpect(jsonPath("$.options[1].active").doesNotExist())
        .andExpect(jsonPath("$.consents[0].id").value("privacy"))
        .andExpect(jsonPath("$.consents[0].text").value("I agree."))
        .andExpect(jsonPath("$.consents[0].mandatory").value(true))
        .andExpect(jsonPath("$.recaptcha.siteKey").value("site-key"))
        .andExpect(jsonPath("$.recaptcha.testMode").value(false));
  }
}
