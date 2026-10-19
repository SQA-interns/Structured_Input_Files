package si.konferenca.registration.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import si.konferenca.registration.application.ExportRegistrations;

class ExportControllerTest {

  final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(
              new ExportController(
                  new ExportRegistrations(List::of, registrations -> new byte[] {80, 75, 3, 4})))
          .build();

  @Test
  void AC_008_01_exportIsAnXlsxAttachment() throws Exception {
    mvc.perform(get("/api/registrations/export"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"registrations.xlsx\""))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().bytes(new byte[] {80, 75, 3, 4}));
  }
}
