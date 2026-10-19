package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Covers AC-004-04: framework errors keep their status and show no details (SB-07). */
class ApiExceptionHandlerTest {

  final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new RegistrationController(null))
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @Test
  void AC_004_04_wrongMethodKeepsItsStatusWithAGenericBody() throws Exception {
    MvcResult result = mvc.perform(get("/api/registrations")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(405);
    assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
        .contains("The request was not accepted.", "\"fieldErrors\":[]")
        .doesNotContain("GET", "Exception");
  }

  @Test
  void AC_004_04_wrongMediaTypeKeepsItsStatus() throws Exception {
    MvcResult result =
        mvc.perform(post("/api/registrations").contentType(MediaType.TEXT_PLAIN).content("x"))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(415);
  }
}
