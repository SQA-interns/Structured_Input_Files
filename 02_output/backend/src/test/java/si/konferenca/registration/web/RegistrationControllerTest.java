package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;

class RegistrationControllerTest {

  static final ConferenceConfiguration CONFIG =
      ConferenceConfiguration.of(
          "Conf",
          List.of(new ConsentDefinition("privacy", "I agree.", true)),
          List.of(new ConferenceOption("ws-a", "Workshop A", OptionCategory.WORKSHOP, true, null)));

  static final String VALID =
      """
      {"type": "EXTERNAL", "firstName": " Ana ", "lastName": "Novak",
       "email": "ana@example.com", "organization": "IJS",
       "optionIds": ["ws-a"], "consentIds": ["privacy"], "recaptchaToken": "ok"}
      """;

  RegistrationStore store = registration -> {};

  MockMvc mvc() {
    RegisterParticipant useCase =
        new RegisterParticipant(
            CONFIG,
            "ok"::equals,
            r -> store.save(r),
            r -> {},
            Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC));
    return MockMvcBuilders.standaloneSetup(new RegistrationController(useCase))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();
  }

  MvcResult send(String body) throws Exception {
    return mvc()
        .perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8)
                .content(body))
        .andReturn();
  }

  @Test
  void AC_001_01_acceptedRegistrationAnswers201WithIdAndLocation() throws Exception {
    mvc()
        .perform(post("/api/registrations").contentType(MediaType.APPLICATION_JSON).content(VALID))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/registrations/[0-9a-f-]{36}")))
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.type").value("EXTERNAL"))
        .andExpect(jsonPath("$.firstName").value("Ana"))
        .andExpect(jsonPath("$.lastName").value("Novak"))
        .andExpect(jsonPath("$.options[0].id").value("ws-a"))
        .andExpect(jsonPath("$.options[0].name").value("Workshop A"))
        .andExpect(jsonPath("$.options[0].category").value("WORKSHOP"))
        .andExpect(jsonPath("$.registeredAt").value("2026-10-19T10:00:00Z"));
  }

  @Test
  void AC_001_02_rejectedSubmissionAnswers400WithFieldErrors() throws Exception {
    mvc()
        .perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("\" Ana \"", "\"  \"")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("The registration has errors."))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"))
        .andExpect(jsonPath("$.fieldErrors[0].message").isString());
  }

  @Test
  void AC_001_07_tokenRejectedByVerifierAnswers400() throws Exception {
    mvc()
        .perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("\"ok\"", "\"bad\"")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("recaptchaToken"));
  }

  @Test
  void AC_001_02_malformedOrUnknownPropertiesAnswer400WithoutDetails() throws Exception {
    for (String body :
        List.of(
            "{\"type\": ",
            VALID.replace("\"type\"", "\"isAdmin\": true, \"type\""),
            VALID.replace("\" Ana \"", "42"))) {
      MvcResult result = send(body);

      assertThat(result.getResponse().getStatus()).as(body).isEqualTo(400);
      String text = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      assertThat(text).contains("\"fieldErrors\":[]").doesNotContain("Exception", "jackson");
    }
  }

  @Test
  void AC_005_03_unexpectedFailureAnswers500WithoutInternalDetails() throws Exception {
    store =
        registration -> {
          throw new IllegalStateException("connection to db-host:5432 refused");
        };

    MvcResult result = send(VALID);

    assertThat(result.getResponse().getStatus()).isEqualTo(500);
    assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
        .contains("Registration could not be completed. Please try again later.")
        .contains("\"fieldErrors\":[]")
        .doesNotContain("db-host", "IllegalState");
  }
}
